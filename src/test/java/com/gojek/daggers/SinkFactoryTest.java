package com.gojek.daggers;

import com.gojek.daggers.sink.InfluxRowSink;
import org.apache.flink.api.common.functions.Function;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SinkFactoryTest {

  @Test
  public void shouldGiveInfluxSinkWhenConfiguredToUseInflux() throws Exception {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getString(eq("SINK_TYPE"), anyString())).thenReturn("influx");
    Function sinkFunction = SinkFactory.getSinkFunction(configuration, new String[]{});

    assertThat(sinkFunction, instanceOf(InfluxRowSink.class));
  }

  @Test
  public void shouldGiveInfluxWhenConfiguredToUseNothing() throws Exception {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getString(eq("SINK_TYPE"), anyString())).thenReturn("");
    Function sinkFunction = SinkFactory.getSinkFunction(configuration, new String[]{});

    assertThat(sinkFunction, instanceOf(InfluxRowSink.class));
  }

  @Test
  public void shouldGiveKafkaProducerWhenConfiguredToUseKafkaSink() throws Exception {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getString(eq("SINK_TYPE"), anyString())).thenReturn("kafka");
    when(configuration.getString(eq("OUTPUT_PROTO_CLASS_PREFIX"), anyString())).thenReturn("output_proto");
    when(configuration.getString(eq("OUTPUT_KAFKA_BROKER"), anyString())).thenReturn("output_broker:2667");
    when(configuration.getString(eq("OUTPUT_KAFKA_TOPIC"), anyString())).thenReturn("output_topic");
    Function sinkFunction = SinkFactory.getSinkFunction(configuration, new String[]{});

    assertThat(sinkFunction, instanceOf(FlinkKafkaProducer010.class));
  }
}