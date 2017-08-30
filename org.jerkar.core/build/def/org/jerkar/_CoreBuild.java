package org.jerkar;

import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaJarBuild;

// Experimental
public class _CoreBuild extends JkJavaJarBuild {

    @Override
    protected _CoreProject project() {
        _CoreProject coreProject = new _CoreProject(baseDir().root());
        return coreProject;
    }

    @Override
    public void doDefault() {
        clean();
        project().doArtifactFile(_CoreProject.DISTRIB_FILE_ID);
        project().doArtifactFile(_CoreProject.ALL_FAT_FILE_ID);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(_CoreBuild.class).doDefault();
    }

}
