/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import org.apache.kafka.common.config.ConfigException;

/**
 * Used to represent a namespaced Kubernetes resource by its group, version, kind, namespace, and name
 */
record NamespacedResourceIdentifier(String group, String version, String kind, String namespace, String name) {
    public static NamespacedResourceIdentifier fromPath(String path)   {
        if (!path.matches("([A-Za-z0-9.-]+/)?([A-Za-z0-9.-]+/)([A-Za-z0-9.-]+/)([A-Za-z0-9.-]+/)[A-Za-z0-9.-]+")) {
            throw new ConfigException("Invalid path " + path + ". It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs).");
        }

        String[] pathSegments = path.split("/");

        if (pathSegments.length == 4)   {
            return new NamespacedResourceIdentifier(null, pathSegments[0], pathSegments[1], pathSegments[2], pathSegments[3]);
        } else if (pathSegments.length == 5)    {
            return new NamespacedResourceIdentifier(pathSegments[0], pathSegments[1], pathSegments[2], pathSegments[3], pathSegments[4]);
        } else  {
            // Should never happen really => the regex should capture all invalid
            // But it handles the missing return error
            throw new ConfigException("Invalid path " + path + ". It has to be in format <group>/<version>/<kind>/<namespace>/<name> (or <group>/<version>/<kind>/<namespace>/<name> for core package APIs).");
        }
    }

    public ResourceDefinitionContext resourceDefinitionContext()    {
        return new ResourceDefinitionContext.Builder()
                .withGroup(group())
                .withVersion(version())
                .withKind(kind())
                .withNamespaced(true)
                .build();
    }
}
