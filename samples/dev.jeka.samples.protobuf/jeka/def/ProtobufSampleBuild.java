import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkSourceGenerator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.protobuf.JkProtocSourceGenerator;

@JkInjectClasspath("../../plugins/dev.jeka.plugins.protobuf/jeka/output/dev.jeka.protobuf-plugin.jar")
class ProtobufSampleBuild extends JkBean {

    ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).configure(this::configure);

    IntellijJkBean intellijJkBean = getBean(IntellijJkBean.class).configureIml(this::configure);

    private void configure(JkIml iml) {
        iml.component.replaceLibByModule("jeka-core.jar", "dev.jeka.core");
        iml.component.replaceLibByModule("protobuf-plugin.jar", "dev.jeka.samples.protobuf");
    }

    private void configure(JkProject project) {
        project.flatFacade()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .setJvmTargetVersion(JkJavaVersion.V8)
                .configureCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:21.0")
                        .and("com.google.protobuf:protobuf-java:3.21.12")
                );
        project.packaging.manifest.addMainClass("Sample");
        project.artifactProducer.putMainArtifact(project.packaging::createFatJar);
        JkSourceGenerator protocGenerator = JkProtocSourceGenerator.of(project, "proto");
        project.compilation.addSourceGenerator(protocGenerator);
    }

    public void cleanPack() {
        projectPlugin.cleanPack();
    }

}