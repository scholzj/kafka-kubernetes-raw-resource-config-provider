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

public class NamespacedResourceIdentifierTest {
    @Test
    public void testResourceIdentifierParsing()    {
        NamespacedResourceIdentifier id = NamespacedResourceIdentifier.fromPath("networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource");

        assertThat(id.group(), is("networking.k8s.io"));
        assertThat(id.version(), is("v1"));
        assertThat(id.kind(), is("NetworkPolicy"));
        assertThat(id.namespace(), is("my-namespace"));
        assertThat(id.name(), is("my-resource"));
    }

    @Test
    public void testCoreResourceIdentifierParsing()    {
        NamespacedResourceIdentifier id = NamespacedResourceIdentifier.fromPath("v1/ConfigMap/my-namespace/my-resource");

        assertThat(id.group(), is(nullValue()));
        assertThat(id.version(), is("v1"));
        assertThat(id.kind(), is("ConfigMap"));
        assertThat(id.namespace(), is("my-namespace"));
        assertThat(id.name(), is("my-resource"));
    }

    @Test
    public void testInvalidResourceIdentifierParsing()    {
        Exception e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("my-namespace/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("/my-namespace"));
        assertThat(e.getMessage(), is("Invalid path /my-namespace. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("my-namespace/my-resource/my-field"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource/my-field. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("my-namespace/my-resource/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource/. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("/my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path /my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));

        e = assertThrows(ConfigException.class, () -> NamespacedResourceIdentifier.fromPath("kubernetes/networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path kubernetes/networking.k8s.io/v1/NetworkPolicy/my-namespace/my-resource. It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs)."));
    }
}
