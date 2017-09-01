package org.jerkar.tool.builtins.javabuild;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkProject;

import java.io.File;

@Deprecated // experimental
public class JkJavaJarBuild extends JkBuild {

    private JkJavaProject project;

    public final JkJavaProject project() {
        if (project == null) {
            project = createProject(this.baseDir().root());
        }
        return project;
    }

    protected JkJavaProject createProject(File baseDir) {
        return new JkJavaProject(this.baseDir().root());
    }

    @Override
    public void doDefault() {
        this.project().doMainJar();
    }

    public void produceAll() {
        this.project().doAllArtifactFiles();
    }
}
