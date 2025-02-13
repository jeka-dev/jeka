import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.tool.JkInject;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 *
 */
class FatJarKBean extends KBean {
    
    @JkInject("../dev.jeka.samples.basic")
    private ProjectKBean basicProjectKBean;

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean.replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.flatFacade.setMainArtifactJarType(JkProjectPackaging.JarType.FAT);
        project.flatFacade.dependencies.compile
                        .add("com.google.guava:guava:33.3.1-jre")
                        .add(basicProjectKBean.project.toDependency());
    }

    public void info() {
        System.out.println(basicProjectKBean.project.getInfo());
    }
   
}
