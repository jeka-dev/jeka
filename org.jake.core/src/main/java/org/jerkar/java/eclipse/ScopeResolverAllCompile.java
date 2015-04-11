package org.jerkar.java.eclipse;

import org.jerkar.depmanagement.JkScope;
import org.jerkar.java.build.JkJavaBuild;
import org.jerkar.java.eclipse.DotClasspath.ClasspathEntry.Kind;

class ScopeResolverAllCompile  implements ScopeResolver {

	@Override
	public JkScope scopeOfLib(Kind kind, String path) {
		return JkJavaBuild.COMPILE;
	}

	@Override
	public JkScope scopeOfCon(String path) {
		return JkJavaBuild.COMPILE;
	}

}
