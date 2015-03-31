package org.jake.java.eclipse;

import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.DotClasspath.ClasspathEntry.Kind;

class ScopeResolverAllCompile  implements ScopeResolver {

	@Override
	public JakeScope scopeOfLib(Kind kind, String path) {
		return JakeJavaBuild.COMPILE;
	}

	@Override
	public JakeScope scopeOfCon(String path) {
		return JakeJavaBuild.COMPILE;
	}

}
