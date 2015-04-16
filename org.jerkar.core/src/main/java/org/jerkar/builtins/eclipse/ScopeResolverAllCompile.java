package org.jerkar.builtins.eclipse;

import org.jerkar.builtins.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.java.build.JkJavaBuild;

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
