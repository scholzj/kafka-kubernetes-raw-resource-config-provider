/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class for Kafka configuration providers using Kubernetes resources
 */
abstract class AbstractKubernetesRawResourceConfigProvider implements ConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesRawResourceConfigProvider.class);

    protected KubernetesClient client;

    /**
     * Creates the configuration provider
     */
    public AbstractKubernetesRawResourceConfigProvider() { }

    // Methods from Kafka Config Provider
    @Override
    public void close() throws IOException {
        LOG.info("Closing Kubernetes Raw Resource config provider");
        client.close();
    }

    @Override
    public void configure(Map<String, ?> config) {
        LOG.info("Configuring Kubernetes Raw Resource config provider with configuration {}", config);
        client = new KubernetesClientBuilder().build();
    }

    @Override
    public ConfigData get(String path) {
        // Not supported
        return new ConfigData(Map.of());
    }

    @Override
    public ConfigData get(String path, Set<String> keys) {
        return getValues(path, keys);
    }

    /**
     * Gets the values from the Kubernetes resource.
     *
     * @param path  Path to the Kubernetes resource
     * @param keys  Keys which should be extracted from the resource
     *
     * @return      Kafka ConfigData with the configuration
     */
    private <T> ConfigData getValues(String path, Set<String> keys)    {
        String jsonResource = new KubernetesSerialization().asJson(getResource(path));
        Map<String, String> configs = new HashMap<>(0);

        for (String key : keys) {
            try {
                T result = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonResource).read(key);

                if (result instanceof String stringResult)   {
                    configs.put(key, stringResult);
                } else if (result instanceof List listResult)  {
                    configs.put(key, listResult.isEmpty() ? null : listResult.get(0).toString());
                } else {
                    throw new ConfigException("Unexpected type " + result.getClass());
                }
            } catch (JsonPathException e)   {
                LOG.error("Failed to query the JSON Path {}", key, e);
                throw new ConfigException("Failed to query the JSON Path " + key);
            }
        }

        return new ConfigData(configs);
    }

    // Kubernetes helper methods

    /**
     * Gets the resource from Kubernetes
     *
     * @param path  Path to the Kubernetes resource
     *
     * @return      Resource retrieved from the Kubernetes cluster
     */
    abstract protected GenericKubernetesResource getResource(String path);
}
