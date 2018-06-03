package org.jerkar.tool.builtins.eclipse;

import org.jerkar.api.ide.eclipse.JkEclipseClasspathApplier;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkPlugin;
import org.jerkar.tool.builtins.java.JkPluginJava;

@JkDoc("Plugin to use Eclipse .classpath file as build definition. When this plugin is activated the .classpath file " +
        "is used over project setting set in the build class.")
public final class JkPluginEclipsePath extends JkPlugin {

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc({ "Flag for resolving dependencies against the eclipse classpath",
        "but trying to segregate test to production code considering path names : ",
    "if path contains 'test' then this is considered as an entry source for scope 'test'." })
    public boolean smartScope = true;

    protected JkPluginEclipsePath(JkBuild build) {
        super(build);
    }

    @Override
    protected void decorateBuild() {
        JkPluginJava pluginJava = build.plugins.get(JkPluginJava.class);
        if (pluginJava != null) {
            final JkJavaProject project = pluginJava.project();
            final JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(smartScope);
            classpathApplier.apply(project);
        } else {
            JkLog.warn("No Java plugin detected in this build : ignore.");
        }
    }

}