package kube;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.LogEvent;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.plugins.springboot.SpringbootJkBean;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

import static dev.jeka.core.api.utils.JkUtilsIterable.listOf;
import static dev.jeka.core.api.utils.JkUtilsIterable.mapOf;
import static java.util.Collections.singletonList;

// see
// - https://github.com/fabric8io/kubernetes-client
// - https://learnk8s.io/spring-boot-kubernetes-guide
// - https://github.com/fabric8io/kubernetes-client/blob/master/doc/CHEATSHEET.md

@JkInjectClasspath("com.google.cloud.tools:jib-core:0.23.0")
@JkInjectClasspath("io.fabric8:kubernetes-client:6.5.1")
@JkInjectClasspath("io.fabric8:kubernetes-client-api:6.5.1")
@JkInjectClasspath("org.slf4j:slf4j-simple:2.0.7")
class Kube extends JkBean {

    private final SpringbootJkBean springboot = getBean(SpringbootJkBean.class);

    private static final String IMAGE_REPO_NAME = "my-images/hello-app";

    Kube() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }

    public void pipeline()  {
        springboot.projectBean.clean();
        springboot.projectBean.compile();
        jibQuietly();
        apply();
    }

    public void apply()  {
        JkLog.startTask("Apply to kube");
        List<HasMetadata> kubeResources = kubeResources();
        kubeResources.forEach(r -> System.out.println(Serialization.asYaml(r)));
        client().resourceList(kubeResources).inNamespace(namespace()).createOrReplace();
        JkLog.endTask();
    }

    public void delete() {
        client().resourceList(kubeResources()).inNamespace(namespace()).delete();
    }

    public void watch() {
        springboot.projectBean.getProject().compilation.layout.resolveSources().watch(2000, this::pipeline);
    }

    @JkDoc("Build Springboot application image")
    public void jib() throws Exception {
        JkLog.startTask("Make image");
        JkProject project = springboot.projectBean.getProject();
        DockerDaemonImage image = DockerDaemonImage.named(IMAGE_REPO_NAME);
        Containerizer containerizer = Containerizer.to(image).addEventHandler(LogEvent.class, this::handleJibLog);
        Jib.from("openjdk:17")
                .addLayer(project.packaging.resolveRuntimeDependencies().getFiles().getEntries(), "/app/libs")
                .addLayer(singletonList(project.compilation.layout.resolveClassDir()), "/app")
                .setEntrypoint("java", "-cp", "/app/classes:/app/libs/*", springboot.getMainClass())
                .containerize(containerizer);
        JkLog.endTask();
    }

    private void jibQuietly() {
        try {
            jib();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<HasMetadata> kubeResources() {
        return listOf(deployment(), service());
    }

    private Deployment deployment() {
       return new DeploymentBuilder(parse(Deployment.class, "deployment.yaml"))
                .editSpec()
                    .editTemplate()
                        .editSpec()
                            .editContainer(0)
                                .withImage(IMAGE_REPO_NAME)
                .endContainer().endSpec().endTemplate().endSpec().build();
    }

    private Service service() {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName("greeting")
                .endMetadata()
                .withNewSpec()
                    .withSelector(mapOf("app", "greeting"))
                    .withPorts(new ServicePortBuilder().withPort(8080).withTargetPort(new IntOrString(8080)).build())
                    .withType("LoadBalancer")
                .endSpec().build();
    }

    private String namespace() {
        return "default";
    }

    private KubernetesClient client() {
        return new KubernetesClientBuilder().build();
    }

    private static <T> T parse(Class<T> targetClass, String resourceName) {
        return new Yaml().loadAs(Kube.class.getResourceAsStream(resourceName), targetClass);
    }

    private void handleJibLog(LogEvent logEvent) {
        System.out.println(logEvent.getLevel() + ": " + logEvent.getMessage());
    }

    public static void main(String[] args) {
        JkInit.instanceOf(Kube.class).apply();
    }

}
