package dev.jeka.core.tool.builtins.eclipse;

import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.eclipse.JkEclipseClasspathApplier;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

@JkDoc("Use Eclipse .classpath file to setup project structure and dependencies.")
@JkDocPluginDeps(JkPluginJava.class)
public final class JkPluginEclipsePath extends JkPlugin {

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc("If true, code belonging to an entry folder having name containing 'test'" +
            " will be considered as test code, so won't be packaged in main jar file.")
    public boolean smartScope = true;

    protected JkPluginEclipsePath(JkCommands run) {
        super(run);
    }

    @JkDoc("Configures java plugin instance in order java project reflects project structure and dependencies described in Eclipse .classpath file.")
    @Override
    protected void activate() {
        JkPluginJava pluginJava = getCommands().getPlugins().get(JkPluginJava.class);
        if (pluginJava != null) {
            final JkJavaProject project = pluginJava.getProject();
            final JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(smartScope);
            classpathApplier.apply(project);
        } else {
            JkLog.warn("No Java plugin detected in this Jeka run : ignore.");
        }
    }

}