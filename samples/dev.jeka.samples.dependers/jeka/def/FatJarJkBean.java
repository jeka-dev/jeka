import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.samples.SimpleProjectJkBean;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectProject;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarJkBean extends JkBean {

    ProjectJkBean projectPlugin = load(ProjectJkBean.class).lately(this::configure);

    final IntellijJkBean intellijJkBean = load(IntellijJkBean.class)
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
    
    @JkInjectProject("../dev.jeka.samples.basic")
    private SimpleProjectJkBean sampleBuild;

    private void configure(JkProject project) {
        project.artifactProducer.putMainArtifact(project.packaging::createFatJar);
        project.flatFacade()
                .configureCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:22.0")
                        .and(sampleBuild.projectPlugin.getProject().toDependency()));
    }
   
}
