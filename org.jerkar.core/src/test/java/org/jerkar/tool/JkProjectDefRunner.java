package org.jerkar.tool;

import org.jerkar.tool.JkProjectDef.JkProjectBuildClassDef;
import org.jerkar.tool.JkProjectDefTest.MyBuild;

/**
 * This class helps to elaborate the help output.
 */
public class JkProjectDefRunner {

    public static void main(String[] args) {
	final JkProjectBuildClassDef def = JkProjectBuildClassDef.of(new MyBuild());
	def.log(true);
    }

}
