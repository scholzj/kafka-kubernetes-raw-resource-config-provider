/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
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

public class KubernetesRawNonNamespacedResourceConfigProviderIT {
    private static final String RESOURCE_NAME = "my-test-resource";

    private static String namespace;
    private static KubernetesClient client;
    private static KubernetesRawNonNamespacedResourceConfigProvider provider;

    @BeforeAll
    public static void beforeAll()   {
        provider = new KubernetesRawNonNamespacedResourceConfigProvider();
        provider.configure(emptyMap());

        client = new KubernetesClientBuilder().build();
        namespace = client.getNamespace();

        ClusterRole cr = new ClusterRoleBuilder()
                .withNewMetadata()
                    .withName(RESOURCE_NAME)
                .endMetadata()
                .withRules(new PolicyRuleBuilder().withApiGroups("my.api").withResources("myres").withVerbs("get").build())
                .build();
        client.rbac().clusterRoles().resource(cr).create();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        client.rbac().clusterRoles().withName(RESOURCE_NAME).delete();
        provider.close();
    }

    @Test
    public void testResource() {
        ConfigData config = provider.get("rbac.authorization.k8s.io/v1/ClusterRole/" + RESOURCE_NAME, Set.of("$.rules[0].apiGroups[0]"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("$.rules[0].apiGroups[0]"), is("my.api"));
    }

    @Test
    public void testCoreResource() {
        ConfigData config = provider.get("v1/Namespace/" + namespace, Set.of("$.status.phase"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("$.status.phase"), is("Active"));
    }

    @Test
    public void testWrongPath() {
        Exception e = assertThrows(ConfigException.class, () -> provider.get("v1/Namespace/" + namespace, Set.of("$.spec.policyTypes[0].type")));
        assertThat(e.getMessage(), is("Failed to query the JSON Path $.spec.policyTypes[0].type"));
    }

    @Test
    public void testWrongResource() {
        Exception e = assertThrows(ConfigException.class, () -> provider.get("v1/Namespace/i-do-not-exist-namespace", Set.of("$.status.phase")));
        assertThat(e.getMessage(), is("Resource null/v1/Namespace with name i-do-not-exist-namespace was not found!"));
    }
}
