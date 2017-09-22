package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.ide.eclipse.JkEclipseClasspathApplier;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuildPlugin2;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkException;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * Plugin to use Eclipse .classpath file as build definition. When this plugin is activated the .classpath
 * file is used over project setting set in the build class.
 */
public final class JkBuildPlugin2EclipseBase extends JkBuildPlugin2<JkJavaProjectBuild> {

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc({ "Flag for resolving dependencies against the eclipse classpath",
        "but trying to segregate test to production code considering path names : ",
    "if path contains 'test' then this is considered as an entry source for scope 'test'." })
    public boolean smartScope = true;

    /**
     * Constructs a JkBuildPlugin2EclipseBase.
     */
    protected JkBuildPlugin2EclipseBase(JkJavaProjectBuild build) {
        super(build);
    }

    @Override
    protected void apply() {
        final JkJavaProject project = build().project();
        JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(smartScope);
        classpathApplier.apply(project);
    }



}