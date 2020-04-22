package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.jacoco.JkPluginJacoco;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkPopularModules.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularModules.JUNIT;

/**
 * This build illustrates how to use Jacoco Plugin. <p>
 *
 * Jacoco plugin simply transforms the {@link dev.jeka.core.api.java.project.JkJavaProject} instance from the
 * Java plugin in order to make it run tests under Jacoco agent to measure test coverage <p>
 *
 * Declaring Jacoco plugin in the build class is not necessary. It can be activated directly from the command line
 * adding <code>jacoco#</code> to the command line. However, if you want to have it enabled by default you need
 * to declare in in the build class.<p>
 *
 * Jacoco report is generated in <i>jeka/output/jacoco</i>.
 */
public class JacocoPluginBuild extends JkCommandSet {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    /*
     * This is not necessary to declare plugins as members. <code>getPlugin(JkPluginJacoco.class)</code>can be
     * invoked in the instance initializer, constructor or #setup method.
     */
    JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    @Override
    protected void setup() {
        javaPlugin.getProject().getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and(GUAVA, "18.0")
                    .and(JUNIT, "4.13", JkScope.TEST));
    }

    public static void main(String[] args) {
        JkInit.instanceOf(JacocoPluginBuild.class, args).javaPlugin.test();
    }

}
