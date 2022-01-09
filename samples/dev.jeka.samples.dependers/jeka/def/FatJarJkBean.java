import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.samples.SimpleProjectJkBean;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectProject;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarJkBean extends JkBean {

    ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).configure(this::configure);
    
    @JkInjectProject("../dev.jeka.samples.basic")
    private SimpleProjectJkBean sampleBuild;

    private void configure(JkProject project) {
        project
            .getArtifactProducer()
                .putMainArtifact(project.getConstruction()::createFatJar)
            .__
            .simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("com.google.guava:guava:22.0")
                        .and(sampleBuild.projectPlugin.getProject().toDependency()));
    }
   
}
