import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.samples.SimpleProjectKBean;
import dev.jeka.core.tool.JkInjectProject;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarKBean extends KBean {
    
    @JkInjectProject("../dev.jeka.samples.basic")
    private SimpleProjectKBean sampleBuild;

    @Override
    protected void init() {

        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");

        JkProject project = load(ProjectKBean.class).project;
        project.artifactProducer.putMainArtifact(project.packaging::createFatJar);
        project.flatFacade()
                .configureCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:22.0")
                        .and(sampleBuild.projectKBean.project.toDependency()));
    }
   
}
