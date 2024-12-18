import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.samples.SimpleProjectKBean;
import dev.jeka.core.tool.JkInjectRunbase;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 *
 */
public class FatJarKBean extends KBean {
    
    @JkInjectRunbase("../dev.jeka.samples.basic")
    private SimpleProjectKBean sampleBuild;

    @Override
    protected void init() {

        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");

        JkProject project = load(ProjectKBean.class).project;
        project.flatFacade.setMainArtifactJarType(JkProjectPackaging.JarType.FAT);
        project.flatFacade.dependencies.compile
                        .add("com.google.guava:guava:22.0")
                        .add(sampleBuild.projectKBean.project.toDependency());
    }
   
}
