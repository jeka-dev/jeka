package org.jerkar.tool.builtins.java;

import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.tool.JkRun;

/**
 * Just provides convenient methods to access Java plugin structure.
 */
@SuppressWarnings("javadoc")
public class JkJavaProjectBuild extends JkRun {

    public final JkPluginJava java() {
        return this.getPlugins().get(JkPluginJava.class);
    }

    public final JkJavaProject project() {
        return java().project();
    }

    public final JkJavaProjectMaker maker() {
        return project().getMaker();
    }

}
