/*
 * Copyright Jakub Scholz.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.kafka;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for Kafka configuration providers using Kubernetes resources
 */
public class KubernetesRawNamespacedResourceConfigProvider extends AbstractKubernetesRawResourceConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesRawNamespacedResourceConfigProvider.class);

    /**
     * Creates the configuration provider
     */
    public KubernetesRawNamespacedResourceConfigProvider() { }

    // Kubernetes helper methods

    /**
     * Gets the resource from Kubernetes
     *
     * @param path  Path to the Kubernetes resource
     *
     * @return      Resource retrieved from the Kubernetes cluster
     */
    @Override
    protected GenericKubernetesResource getResource(String path)   {
        final NamespacedResourceIdentifier ri = NamespacedResourceIdentifier.fromPath(path);

        LOG.info("Retrieving resource {}/{} {} with name {} from namespace {}", ri.group(), ri.version(), ri.kind(), ri.name(), ri.namespace());

        try {
            GenericKubernetesResource resource = client.genericKubernetesResources(ri.resourceDefinitionContext()).inNamespace(ri.namespace()).withName(ri.name()).get();

            if (resource == null)   {
                LOG.error("Resource {}/{} {} with name {} from namespace {} was not found", ri.group(), ri.version(), ri.kind(), ri.name(), ri.namespace());
                throw new ConfigException("Resource " + ri.group() + "/" + ri.version() + "/" + ri.kind() + " with name " + ri.name() + " was not found in namespace " + ri.namespace() + "!");
            }

            return resource;
        } catch (KubernetesClientException e)   {
            LOG.error("Failed to retrieve resource {}/{} {} with name {} from namespace {}", ri.group(), ri.version(), ri.kind(), ri.name(), ri.namespace(), e);
            throw new ConfigException("Failed to retrieve resource " + ri.group() + "/" + ri.version() + "/" + ri.kind() + " with name " + ri.name() + " was not found in namespace " + ri.namespace() + "!", e);
        }
    }
}
