/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KubernetesRawNamespacedResourceConfigProviderIT {
    private static final String RESOURCE_NAME = "my-test-resource";

    private static String namespace;
    private static KubernetesClient client;
    private static KubernetesRawNamespacedResourceConfigProvider provider;

    @BeforeAll
    public static void beforeAll()   {
        provider = new KubernetesRawNamespacedResourceConfigProvider();
        provider.configure(emptyMap());

        client = new KubernetesClientBuilder().build();
        namespace = client.getNamespace();

        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(RESOURCE_NAME)
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withSelector(Map.of("label", "value"))
                    .withPorts(new ServicePortBuilder().withName("web").withProtocol("TCP").withPort(80).withTargetPort(new IntOrString(8080)).build())
                .endSpec()
                .build();
        client.services().resource(service).create();

        NetworkPolicy np = new NetworkPolicyBuilder()
                .withNewMetadata()
                    .withName(RESOURCE_NAME)
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withNewPodSelector()
                        .withMatchLabels(Map.of("label", "value"))
                    .endPodSelector()
                    .withIngress(new NetworkPolicyIngressRuleBuilder().build())
                    .withPolicyTypes("Ingress")
                .endSpec()
                .build();
        client.network().networkPolicies().resource(np).create();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        client.services().inNamespace(namespace).withName(RESOURCE_NAME).delete();
        client.network().networkPolicies().inNamespace(namespace).withName(RESOURCE_NAME).delete();
        provider.close();
    }

    @Test
    public void testKafkaResource() {
        ConfigData config = provider.get("kafka.strimzi.io/v1beta2/Kafka/myproject/my-cluster", Set.of("$.status.listeners[?(@.name=='external')].bootstrapServers"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));

        data.forEach((k, v) -> System.out.println(k + ": " + v));

        assertThat(data.get("$.status.listeners[?(@.name=='external')].bootstrapServers"), is("192.168.1.73:32167"));
    }

    @Test
    public void testResource() {
        ConfigData config = provider.get("networking.k8s.io/v1/NetworkPolicy/" + namespace + "/" + RESOURCE_NAME, Set.of("$.spec.policyTypes[0]"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("$.spec.policyTypes[0]"), is("Ingress"));
    }

    @Test
    public void testCoreResource() {
        ConfigData config = provider.get("v1/Service/" + namespace + "/" + RESOURCE_NAME, Set.of("$.spec.type"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("$.spec.type"), is("ClusterIP"));
    }

    @Test
    public void testWrongPath() {
        Exception e = assertThrows(ConfigException.class, () -> provider.get("networking.k8s.io/v1/NetworkPolicy/" + namespace + "/" + RESOURCE_NAME, Set.of("$.spec.policyTypes[0].type")));
        assertThat(e.getMessage(), is("Failed to query the JSON Path $.spec.policyTypes[0].type"));
    }

    @Test
    public void testWrongResource() {
        Exception e = assertThrows(ConfigException.class, () -> provider.get("v1/Service/" + namespace + "/i-do-not-exist-service", Set.of("$.spec.type")));
        assertThat(e.getMessage(), is("Resource null/v1/Service with name i-do-not-exist-service was not found in namespace myproject!"));
    }
}
