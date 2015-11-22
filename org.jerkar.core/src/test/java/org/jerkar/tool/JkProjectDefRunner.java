package org.jerkar.tool;

import org.jerkar.tool.JkProjectDefTest.MyBuild;
import org.jerkar.tool.ProjectDef.ProjectBuildClassDef;

/**
 * This class helps to elaborate the help output.
 */
public class JkProjectDefRunner {

    public static void main(String[] args) {
        final ProjectBuildClassDef def = ProjectBuildClassDef.of(new MyBuild());
        def.log(true);
    }

}
