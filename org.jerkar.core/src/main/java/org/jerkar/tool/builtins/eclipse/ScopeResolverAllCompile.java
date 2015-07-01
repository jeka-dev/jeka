package org.jerkar.tool.builtins.eclipse;

import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.tool.builtins.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

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
