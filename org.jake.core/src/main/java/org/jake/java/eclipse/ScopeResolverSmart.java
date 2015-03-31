package org.jake.java.eclipse;

import java.io.File;

import org.jake.JakeLog;
import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.DotClasspath.ClasspathEntry;
import org.jake.java.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jake.utils.JakeUtilsString;

class ScopeResolverSmart implements ScopeResolver {

	private final WstCommonComponent wstCommonComponent;

	public ScopeResolverSmart(WstCommonComponent wstCommonComponent) {
		super();
		this.wstCommonComponent = wstCommonComponent;
	}


	@Override
	public JakeScope scopeOfLib(Kind kind, String path) {
		JakeScope scope = scopeOfLibAccordingLocation(new File(path));
		if (wstCommonComponent != null) {
			final ClasspathEntry classpathEntry = ClasspathEntry.of(kind, path);
			if (!wstCommonComponent.contains(classpathEntry)) {
				if (scope.isInOrIsExtendingAnyOf(JakeJavaBuild.COMPILE)) {
					JakeLog.trace(path + " not found as module in " + WstCommonComponent.FILE + " : turn scope to 'provided'.");
					scope = JakeJavaBuild.PROVIDED;
				}
			}
		}
		return scope;
	}

	private JakeScope scopeOfLibAccordingLocation(File libFile) {
		final String parent = libFile.getParentFile().getName();
		if (parent.equalsIgnoreCase(JakeJavaBuild.COMPILE.name())) {
			return JakeJavaBuild.COMPILE;
		}
		if (parent.equalsIgnoreCase(JakeJavaBuild.TEST.name())) {
			return JakeJavaBuild.TEST;
		}
		if (parent.equalsIgnoreCase(JakeJavaBuild.PROVIDED.name())) {
			return JakeJavaBuild.PROVIDED;
		}
		if (parent.equalsIgnoreCase(JakeJavaBuild.RUNTIME.name())) {
			return JakeJavaBuild.RUNTIME;
		}
		final String path = libFile.getPath();
		final String name = path.contains("/") ? JakeUtilsString.substringAfterLast(path, "/") : path;
		if (name.toLowerCase().contains("junit")) {
			return JakeJavaBuild.TEST;
		}
		if (name.toLowerCase().contains("lombok")) {
			return JakeJavaBuild.PROVIDED;
		}
		return JakeJavaBuild.COMPILE;
	}



	@Override
	public JakeScope scopeOfCon(String path) {
		if (path.contains("org.eclipse.jdt.junit.JUNIT_CONTAINER")) {
			return JakeJavaBuild.TEST;
		}
		return JakeJavaBuild.COMPILE;
	}



}
