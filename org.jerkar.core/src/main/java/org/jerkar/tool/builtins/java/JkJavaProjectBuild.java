package org.jerkar.tool.builtins.java;

import org.jerkar.tool.JkBuild;

/**
 * Build configured with a Java plugin.
 */
@SuppressWarnings("javadoc")
public abstract class JkJavaProjectBuild extends JkBuild {

    protected JkJavaProjectBuild() {
        super();
        java(); // this.plugins.get(JkPluginJava.class) has to be called once to be active.
    }

    public final JkPluginJava java() {
        return this.plugins.get(JkPluginJava.class);
    }

}
