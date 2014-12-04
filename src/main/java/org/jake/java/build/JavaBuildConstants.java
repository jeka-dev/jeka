package org.jake.java.build;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.PROVIDED;
import static org.jake.java.build.JakeBuildJava.RUNTIME;
import static org.jake.java.build.JakeBuildJava.TEST;

import org.jake.depmanagement.JakeScopeMapping;

class JavaBuildConstants {

	private static final String MASTER = "master";

	public static JakeScopeMapping defaultMapping() {
		return JakeScopeMapping.of(COMPILE).to(COMPILE.name(), MASTER)
				.and(RUNTIME).to(RUNTIME.name(), MASTER)
				.and(TEST).to(TEST.name(), MASTER)
				.and(PROVIDED).to(PROVIDED.name(), MASTER);
	}

}
