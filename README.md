[![GitHub release](https://img.shields.io/github/release/scholzj/kafka-kubernetes-raw-resource-config-provider.svg)](https://github.com/scholzj/kafka-kubernetes-raw-resource-config-provider/releases/latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cz.scholz/kafka-kubernetes-raw-resource-config-provider/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cz.scholz/kafka-kubernetes-raw-resource-config-provider)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/scholzj?style=social)](https://twitter.com/scholzj)

# Kubernetes Raw Resource Configuration Provider for Apache Kafka®

[Apache Kafka®](https://kafka.apache.org) supports pluggable configuration providers which can load configuration data from external sources.
This configuration provider allows you to read fields from any Kubernetes resource using a JSON Path query.


_Note: If you want to load data from Secrets or Config Maps, you should also consider using the [Strimzi Kubernetes Config Provider](https://github.com/strimzi/kafka-kubernetes-config-provider)._

## Using the provider

The provider consists of two classes: `KubernetesRawNamespacedResourceConfigProvider` and `KubernetesRawNonNamespacedResourceConfigProvider`.
`KubernetesRawNamespacedResourceConfigProvider` is for getting information from namespace scoped resources.
`KubernetesRawNonNamespacedResourceConfigProvider` is for getting information from cluster scoped resources.
You can initialize them as any other Apache Kafka config provider:

```properties
config.providers=namespaced,cluster
config.providers.namespaced.class=cz.scholz.kafka.KubernetesRawNamespacedResourceConfigProvider
config.providers.cluster.class=cz.scholz.kafka.KubernetesRawNonNamespacedResourceConfigProvider
```

And once you have initialized them, you can use them.
In the example below, it is used to extract the bootstrap server address of a listener named `external` from a status of the [Strimzi `Kafka` custom resource](https://strimzi.io):

```properties
bootstrap.servers=${namespaced:kafka.strimzi.io/v1beta2/Kafka/myproject/my-cluster:$.status.listeners[?(@.name=='external')].bootstrapServers}
```

The configuration of the config provider consists of two parts:
1) The part that specifies the Kubernetes resource.
   In the example above, this is `kafka.strimzi.io/v1beta2/Kafka/myproject/my-cluster`.
   The structure for namespaced resources is `<group>/<version>/<kind>/<namespace>/<name>`.
   So the example above queries the custom resource from the API group `kafka.strimzi.io`, API version `v1beta2`, the resource kind `Kafka`, from the namespace `myproject`, and with the name `my-cluster`.
   For resources from the Kubernetes `core` API, you can omit the API Group and use only `<version>/<kind>/<namespace>/<name>`.
   For example `v1/Service/my-namespace/my-service`.
   The resource specification for cluster-scoped resource is the same but without the namespace.1
   I.e. `<group>/<version>/<kind>/<name>`.
   And for `core` APIs yuo can again omit the API group: `<version>/<kind>/<name>`.
2) The second part is the JSON Path that will be used to query the Kubernetes resource.
   In the example above, this is `$.status.listeners[?(@.name=='external')].bootstrapServers`.
   The JSON Path is executed against the Kubernetes resource using the [Jayway JsonPath](https://github.com/json-path/JsonPath).
   So it should follow all its rules.

## Installation

To use this config provider, you can use the JARs from the archive attached to one of the GitHub releases.
Or you can download it as a Java dependency from Maven Central:

```xml
<dependency>
    <groupId>cz.scholz</groupId>
    <artifactId>kafka-kubernetes-raw-resource-config-provider</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Configuring the Kubernetes client

The Kubernetes Config Provider is using the [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client).
You can configure it using Java system properties, environment variables, Kube config files or ServiceAccount tokens.
All available configuration options are described in the [client documentation](https://github.com/fabric8io/kubernetes-client#configuring-the-client).
By default, it will try to automatically find the available configuration - for example from the Kube config file (`~/.kube/config`) or from the ServiceAccount if running inside Kubernetes Pod.

### RBAC rights

The Kubernetes account used by the Kubernetes Raw Resource Configuration Provider needs to have access to the resources you will query.
The only RBAC rights it needs is the `get` access rights on a given resource.
For example:

```yaml
- apiGroups: ["kafka.strimzi.io"]
  resources: ["kafkas"]
  resourceNames: ["my-cluster"]
  verbs: ["get"]
```

It does not need any other access rights.
