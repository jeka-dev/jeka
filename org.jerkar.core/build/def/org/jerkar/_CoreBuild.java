package org.jerkar;

import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaJarBuild;

import java.io.File;

// Experimental
public class _CoreBuild extends JkJavaJarBuild {

    @Override
    protected _CoreProject createProject(File baseDir) {
        _CoreProject coreProject = new _CoreProject(baseDir);
        coreProject.setVersionedModule("org.jerkar:core", "0.7-SNAPSHOT");
        return coreProject;
    }

    @Override
    public void doDefault() {
        project().maker().clean();
        project().makeAllArtifactFiles();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(_CoreBuild.class).doDefault();
    }

}
