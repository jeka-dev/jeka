import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.samples.JavaPluginBuild;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkPopularModules.JUNIT;

/**
 * Simple build demonstrating how Jerkar can handle multi-project build.
 * <p>
 * Here, the project depends on the <code>org.jerkar.samples</code> getSibling project.
 * More precisely, on the jar file produced by <code>MavenStyleBuild</code>.
 * <p>
 * Compilation depends on a jar produced by <code>org.jerkar.samples</code>
 * project and from its transitive dependencies.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class NormalJarBuild extends JkCommandSet {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    /*
     *  Creates a sample build instance of the 'org.jerkar.samples' project.
     *  The 'samples' project path must be relative to this one.
     *  So in this case, the two projects are supposed to lie in the same folder.
     */
    @JkDefImport("../dev.jeka.core-samples")
    private JavaPluginBuild sampleBuild;

    @Override
    protected void setup() {
        javaPlugin.getProject()
            .getArtifactProducer()
                .putMainArtifact(javaPlugin.getProject().getPackaging()::createFatJar).__
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and(JUNIT, "4.13", JkJavaDepScopes.TEST)
                    .and(sampleBuild.java.getProject()));
    }

    public void cleanPack() {
        clean();
        javaPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(NormalJarBuild.class).cleanPack();
    }

}
