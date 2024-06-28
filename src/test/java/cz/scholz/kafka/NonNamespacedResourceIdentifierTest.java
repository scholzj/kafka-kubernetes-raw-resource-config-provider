/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NonNamespacedResourceIdentifierTest {
    @Test
    public void testResourceIdentifierParsing()    {
        NonNamespacedResourceIdentifier id = NonNamespacedResourceIdentifier.fromPath("rbac.authorization.k8s.io/v1/ClusterRole/my-resource");

        assertThat(id.group(), is("rbac.authorization.k8s.io"));
        assertThat(id.version(), is("v1"));
        assertThat(id.kind(), is("ClusterRole"));
        assertThat(id.name(), is("my-resource"));
    }

    @Test
    public void testCoreResourceIdentifierParsing()    {
        NonNamespacedResourceIdentifier id = NonNamespacedResourceIdentifier.fromPath("v1/Node/my-resource");

        assertThat(id.group(), is(nullValue()));
        assertThat(id.version(), is("v1"));
        assertThat(id.kind(), is("Node"));
        assertThat(id.name(), is("my-resource"));
    }

    @Test
    public void testInvalidResourceIdentifierParsing()    {
        Exception e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("my-namespace/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("/my-namespace"));
        assertThat(e.getMessage(), is("Invalid path /my-namespace. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("my-namespace/my-resource/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource/. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("/my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path /my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));
        
        e = assertThrows(ConfigException.class, () -> NonNamespacedResourceIdentifier.fromPath("kubernetes/networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path kubernetes/networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<name> (or <group>/<version>/<kind>/<name> for core package APIs)."));
    }
}
