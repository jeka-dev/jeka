import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.samples.SimpleProjectJkBean;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

/**
 * Simple build demonstrating how Jeka can handle multi-project build.
 * <p>
 * Here, the project depends on the <code>dev.jeka.core-samples</code> sibling project.
 * <p>
 * Compilation depends on a jar produced by <code>dev.jeka.core-samples</code>
 * project and from its transitive dependencies.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class NormalJarJkBean extends JkBean {

    ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).lately(this::configure);

    /*
     *  Creates a sample build instance of the 'org.jerkar.samples' project.
     *  The 'samples' project path must be relative to this one.
     *  So in this case, the two projects are supposed to lie in the same folder.
     */
    @JkInjectProject("../dev.jeka.samples.basic")
    private SimpleProjectJkBean sampleBuild;


    private void configure(JkProject project) {
        project.artifactProducer.putMainArtifact(project.packaging::createFatJar);
        project.compilation.configureDependencies(deps -> deps
                .and(sampleBuild.projectPlugin.getProject().toDependency())
        );
    }

    public void cleanPack() {
        cleanOutput();
        projectPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(NormalJarJkBean.class).cleanPack();
    }

}
