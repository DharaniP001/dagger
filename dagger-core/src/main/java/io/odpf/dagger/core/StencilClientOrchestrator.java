package io.odpf.dagger.core;

import org.apache.flink.configuration.Configuration;

import com.gojek.de.stencil.StencilClientFactory;
import com.gojek.de.stencil.client.StencilClient;
import io.odpf.dagger.core.utils.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class StencilClientOrchestrator implements Serializable {
    private static StencilClient stencilClient;
    private Configuration configuration;
    private HashMap<String, String> stencilConfigMap;
    private HashSet<String> stencilUrls;

    public StencilClientOrchestrator(Configuration configuration) {
        this.configuration = configuration;
        this.stencilConfigMap = createStencilConfigMap(configuration);
        this.stencilUrls = getStencilUrls();
    }

    private HashMap<String, String> createStencilConfigMap(Configuration configuration) {
        stencilConfigMap = new HashMap<>();
        stencilConfigMap.put(Constants.STENCIL_CONFIG_REFRESH_CACHE_KEY, configuration.getString(Constants.STENCIL_CONFIG_REFRESH_CACHE_KEY, Constants.STENCIL_CONFIG_REFRESH_CACHE_DEFAULT));
        stencilConfigMap.put(Constants.STENCIL_CONFIG_TTL_IN_MINUTES_KEY, configuration.getString(Constants.STENCIL_CONFIG_TTL_IN_MINUTES_KEY, Constants.STENCIL_CONFIG_TTL_IN_MINUTES_DEFAULT));
        stencilConfigMap.put(Constants.STENCIL_CONFIG_TIMEOUT_MS_KEY, configuration.getString(Constants.STENCIL_CONFIG_TIMEOUT_MS_KEY, Constants.STENCIL_CONFIG_TIMEOUT_MS_DEFAULT));
        return stencilConfigMap;
    }

    public StencilClient getStencilClient() {
        if (stencilClient != null) {
            return stencilClient;
        }

        stencilClient = initStencilClient(new ArrayList<>(stencilUrls));
        return stencilClient;
    }

    public StencilClient enrichStencilClient(List<String> additionalStencilUrls) {
        if (additionalStencilUrls.isEmpty()) {
            return stencilClient;
        }

        stencilUrls.addAll(additionalStencilUrls);
        stencilClient = initStencilClient(new ArrayList<>(stencilUrls));
        return stencilClient;
    }

    private StencilClient initStencilClient(List<String> stencilUrls) {
        boolean enableRemoteStencil = configuration.getBoolean(Constants.STENCIL_ENABLE_KEY, Constants.STENCIL_ENABLE_DEFAULT);
        return enableRemoteStencil
                ? StencilClientFactory.getClient(stencilUrls, stencilConfigMap)
                : StencilClientFactory.getClient();
    }

    private HashSet<String> getStencilUrls() {
        stencilUrls = Arrays.stream(configuration.getString(Constants.STENCIL_URL_KEY, Constants.STENCIL_URL_DEFAULT).split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
        return stencilUrls;
    }
}