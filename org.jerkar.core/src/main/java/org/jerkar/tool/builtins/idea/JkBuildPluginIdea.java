package org.jerkar.tool.builtins.idea;

import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkBuildPluginIdea extends JkJavaBuildPlugin {

    private JkBuild build;

    @Override
    public void configure(JkBuild build) {
        this.build = build;
    }

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file")
    public void generateFiles() {

    }


}