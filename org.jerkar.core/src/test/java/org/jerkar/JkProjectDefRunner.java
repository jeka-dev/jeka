package org.jerkar;

import org.jerkar.JkProjectDef.JkProjectBuildClassDef;
import org.jerkar.JkProjectDefTest.MyBuild;

/**
 * This class helps to elaborate the help output.
 */
public class JkProjectDefRunner {

	public static void main(String[] args) {
		final JkProjectBuildClassDef def = JkProjectBuildClassDef.of(MyBuild.class);
		def.log(true);
	}

}
