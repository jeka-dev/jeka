package org.jerkar.tool.builtins.javabuild;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;

import java.io.File;

// experimental
public class JkJavaProjectBuild extends JkBuild {

    private JkJavaProject project;

    public final JkJavaProject project() {
        if (project == null) {
            project = createProject(this.baseTree().root());
        }
        return project;
    }

    protected JkJavaProject createProject(File baseDir) {
        return new JkJavaProject(this.baseTree().root());
    }

    @Override
    public void doDefault() {
        this.project().makeMainJar();
    }

    @Override
    public JkFileTree ouputTree() {
        return JkFileTree.of(this.project().getOutLayout().outputDir());
    }

    public void produceAll() {
        this.project().makeAllArtifactFiles();
    }


}
