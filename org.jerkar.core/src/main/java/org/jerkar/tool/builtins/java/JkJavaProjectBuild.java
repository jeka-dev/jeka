package org.jerkar.tool.builtins.java;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectMaker;
import org.jerkar.tool.JkBuild;

/**
 * Build configured with a Java plugin.
 */
@SuppressWarnings("javadoc")
public class JkJavaProjectBuild extends JkBuild {

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
