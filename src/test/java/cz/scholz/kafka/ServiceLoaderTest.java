/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import org.apache.kafka.common.config.provider.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceLoaderTest {
    @Test
    public void testServiceLoaderDiscovery() {
        ServiceLoader<ConfigProvider> serviceLoader = ServiceLoader.load(ConfigProvider.class);

        boolean namespacedProviderDiscovered = false;
        boolean nonNamespacedProviderDiscovered = false;

        for (ConfigProvider service : serviceLoader)    {
            System.out.println(service.getClass());
            if (service instanceof KubernetesRawNamespacedResourceConfigProvider) {
                namespacedProviderDiscovered = true;
            } else if (service instanceof KubernetesRawNonNamespacedResourceConfigProvider) {
                nonNamespacedProviderDiscovered = true;
            }
        }

        assertTrue(namespacedProviderDiscovered);
        assertTrue(nonNamespacedProviderDiscovered);
    }
}
