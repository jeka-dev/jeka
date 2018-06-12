package org.jerkar.tool.builtins.eclipse;

import org.jerkar.api.ide.eclipse.JkEclipseClasspathApplier;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkDocPluginDeps;
import org.jerkar.tool.JkPlugin;
import org.jerkar.tool.builtins.java.JkPluginJava;

@JkDoc("Use Eclipse .classpath file to configure project structure and dependencies.")
@JkDocPluginDeps(JkPluginJava.class)
public final class JkPluginEclipsePath extends JkPlugin {

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc("If true, code belonging to an entry folder having name containing 'test'" +
            " will be considered as test code, so won't be packaged in main jar file.")
    public boolean smartScope = true;

    protected JkPluginEclipsePath(JkBuild build) {
        super(build);
    }

    @JkDoc("Configure java plugin instance in order java project reflects project structure and dependencies described in Eclipse .classpath file.")
    @Override
    protected void decorateBuild() {
        JkPluginJava pluginJava = build.plugins.get(JkPluginJava.class);
        if (pluginJava != null) {
            final JkJavaProject project = pluginJava.project();
            final JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(smartScope);
            classpathApplier.apply(project);
        } else {
            JkLog.warn(this,"No Java plugin detected in this build : ignore.");
        }
    }

}