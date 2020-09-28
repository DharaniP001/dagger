package com.gojek.daggers.postProcessors;

import com.gojek.daggers.core.StencilClientOrchestrator;
import com.gojek.daggers.core.StreamInfo;
import com.gojek.daggers.metrics.telemetry.TelemetrySubscriber;
import com.gojek.daggers.postProcessors.common.ColumnNameManager;
import com.gojek.daggers.postProcessors.common.PostProcessor;
import com.gojek.daggers.postProcessors.external.ExternalMetricConfig;
import com.gojek.daggers.postProcessors.external.ExternalPostProcessor;
import com.gojek.daggers.postProcessors.external.SchemaConfig;
import com.gojek.daggers.postProcessors.external.common.FetchOutputDecorator;
import com.gojek.daggers.postProcessors.external.common.InitializationDecorator;
import com.gojek.daggers.postProcessors.internal.InternalPostProcessor;
import com.gojek.daggers.postProcessors.transfromers.TransformProcessor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.gojek.daggers.utils.Constants.POST_PROCESSOR_ENABLED_KEY;
import static com.gojek.daggers.utils.Constants.POST_PROCESSOR_ENABLED_KEY_DEFAULT;

public class ParentPostProcessor implements PostProcessor {
    private final PostProcessorConfig postProcessorConfig;
    private final StencilClientOrchestrator stencilClientOrchestrator;
    private TelemetrySubscriber telemetrySubscriber;
    private Configuration configuration;

    public ParentPostProcessor(PostProcessorConfig postProcessorConfig, Configuration configuration, StencilClientOrchestrator stencilClientOrchestrator, TelemetrySubscriber telemetrySubscriber) {
        this.postProcessorConfig = postProcessorConfig;
        this.configuration = configuration;
        this.stencilClientOrchestrator = stencilClientOrchestrator;
        this.telemetrySubscriber = telemetrySubscriber;
    }

    @Override
    public StreamInfo process(StreamInfo streamInfo) {
        if (!canProcess(postProcessorConfig)) {
            return streamInfo;
        }
        DataStream<Row> resultStream = streamInfo.getDataStream();
        ColumnNameManager columnNameManager = new ColumnNameManager(streamInfo.getColumnNames(), postProcessorConfig.getOutputColumnNames());

        InitializationDecorator initializationDecorator = new InitializationDecorator(columnNameManager);
        resultStream = initializationDecorator.decorate(resultStream);
        streamInfo = new StreamInfo(resultStream, streamInfo.getColumnNames());

        List<PostProcessor> enabledPostProcessors = getEnabledPostProcessors(stencilClientOrchestrator, columnNameManager, telemetrySubscriber);
        for (PostProcessor postProcessor : enabledPostProcessors) {
            streamInfo = postProcessor.process(streamInfo);
        }

        FetchOutputDecorator fetchOutputDecorator = new FetchOutputDecorator();
        resultStream = fetchOutputDecorator.decorate(streamInfo.getDataStream());
        StreamInfo resultantStreamInfo = new StreamInfo(resultStream, columnNameManager.getOutputColumnNames());
        TransformProcessor transformProcessor = new TransformProcessor(postProcessorConfig.getTransformers());
        if (transformProcessor.canProcess(postProcessorConfig)) {
            transformProcessor.notifySubscriber(telemetrySubscriber);
            resultantStreamInfo = transformProcessor.process(resultantStreamInfo);
        }
        return resultantStreamInfo;
    }

    @Override
    public boolean canProcess(PostProcessorConfig postProcessorConfig) {
        return postProcessorConfig != null && !postProcessorConfig.isEmpty();
    }

    private List<PostProcessor> getEnabledPostProcessors(StencilClientOrchestrator stencilClientOrchestrator, ColumnNameManager columnNameManager, TelemetrySubscriber telemetrySubscriber) {
        if (!configuration.getBoolean(POST_PROCESSOR_ENABLED_KEY, POST_PROCESSOR_ENABLED_KEY_DEFAULT)) {
            return new ArrayList<>();
        }

        SchemaConfig schemaConfig = new SchemaConfig(configuration, stencilClientOrchestrator, columnNameManager);
        ExternalMetricConfig externalMetricConfig = getExternalMetricConfig(configuration, telemetrySubscriber);
        ArrayList<PostProcessor> postProcessors = new ArrayList<>();
        postProcessors.add(new ExternalPostProcessor(schemaConfig, postProcessorConfig.getExternalSource(), externalMetricConfig));
        postProcessors.add(new InternalPostProcessor(postProcessorConfig));
        return postProcessors
                .stream()
                .filter(p -> p.canProcess(postProcessorConfig))
                .collect(Collectors.toList());
    }

    private ExternalMetricConfig getExternalMetricConfig(Configuration configuration, TelemetrySubscriber telemetrySubscriber) {
        return new ExternalMetricConfig(configuration, telemetrySubscriber);
    }

}
