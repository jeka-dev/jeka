package org.jerkar.tool.builtins.javabuild;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkBuild;

import java.io.File;

@Deprecated // experimental
public class JkJavaProjectBuild extends JkBuild {

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
        this.project().makeMainJar();
    }

    public void produceAll() {
        this.project().makeAllArtifactFiles();
    }

    @Override
    public void clean() {
        super.clean();
        File projectOutDir = this.project().getOutLayout().outputDir();
        if (!JkUtilsFile.isSame(ouputDir().root(), projectOutDir)) {
            JkLog.start("Cleaning output directory " + projectOutDir);
            project().maker().clean();
            JkLog.done();
        }


    }
}
