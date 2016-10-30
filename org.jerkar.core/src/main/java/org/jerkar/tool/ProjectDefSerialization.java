package org.jerkar.tool;

import java.io.File;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.ProjectDef.JkProjectBuildMethodDef;
import org.jerkar.tool.ProjectDef.ProjectBuildClassDef;

/**
 * Provides a main method that output project metadata (as
 *
 */
class ProjectDefSerialization {

    public static void main(String[] args) {
        final File workingDir = JkUtilsFile.workingDir();
        final Project project = new Project(workingDir);
        final Object build = project.instantiate(JkInit.of(new String[] {"-silent=true"}));
        final ProjectBuildClassDef buildClassDef = ProjectBuildClassDef.of(build);
        for (final JkProjectBuildMethodDef methodDef : buildClassDef.methodDefinitions() ) {
            System.out.print(methodDef.serialize());
            System.out.print("||");
        }
    }

}
