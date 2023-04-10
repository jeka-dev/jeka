package kube;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.LogEvent;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.plugins.springboot.SpringbootJkBean;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.Map;

import static dev.jeka.core.api.utils.JkUtilsIterable.mapOf;
import static java.util.Collections.singletonList;

@JkInjectClasspath("com.google.cloud.tools:jib-core:0.23.0")
@JkInjectClasspath("io.fabric8:kubernetes-client:6.5.1")
@JkInjectClasspath("io.fabric8:kubernetes-client-api:6.5.1")
@JkInjectClasspath("io.fabric8:kubernetes-model:6.5.1")
@JkInjectClasspath("org.slf4j:slf4j-simple:2.0.7")
class Kube extends JkBean {

    private final SpringbootJkBean springboot = getBean(SpringbootJkBean.class);

    private static final String IMAGE_REPO_NAME = "my-images/hello-app";

    public static void main(String[] args) {
        JkInit.instanceOf(Kube.class, args, "kube");
    }

    {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }

    public void jib() throws Exception {
        JkProject project = springboot.projectBean.getProject();
        DockerDaemonImage image = DockerDaemonImage.named(IMAGE_REPO_NAME);
        Containerizer containerizer = Containerizer.to(image).addEventHandler(LogEvent.class,
                logEvent -> System.out.println(logEvent.getLevel() + ": " + logEvent.getMessage()));
        Jib.from("openjdk:17")
                .addLayer(project.packaging.resolveRuntimeDependencies().getFiles().getEntries(), "/app/libs")
                .addLayer(singletonList(project.compilation.layout.resolveClassDir()), "/app")
                .setEntrypoint("java", "-cp", "/app/classes:/app/libs/*", springboot.getMainClass())
                .containerize(containerizer);
    }

    // see https://github.com/fabric8io/kubernetes-client
    // https://learnk8s.io/spring-boot-kubernetes-guide
    // https://github.com/fabric8io/kubernetes-client/blob/master/doc/CHEATSHEET.md
    public void apply() throws Exception {
        Map<String, String> labels = mapOf("app", "hello");
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName("hello-deployment")
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .addToMatchLabels(mapOf("app", "hello"))
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", "hello")
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainerLike(serverContainer("hello-server", IMAGE_REPO_NAME, 8080))
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec().build();
        KubernetesClient client = new KubernetesClientBuilder().build();
        client.apps().deployments().inNamespace("default").createOrReplace(deployment);
    }

    private Container serverContainer(String name, String image, int port) {
        return new ContainerBuilder()
                .withName(name)
                .withImage(image)
                .addNewPort()
                    .withContainerPort(port)
                .endPort()
                .build();
    }

}
