package org.jerkar.tool.builtins.java;

import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.tool.JkRun;

/**
 * Build configured with a Java plugin.
 */
@SuppressWarnings("javadoc")
public class JkJavaProjectBuild extends JkRun {

    @Override
    protected void afterOptionsInjected() {
        java();
    }

    public final JkPluginJava java() {
        return this.plugins().get(JkPluginJava.class);
    }

    public final JkJavaProject project() {
        return java().project();
    }

    public final JkJavaProjectMaker maker() {
        return project().maker();
    }

}
