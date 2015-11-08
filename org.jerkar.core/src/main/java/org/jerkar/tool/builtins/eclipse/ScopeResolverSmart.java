package org.jerkar.tool.builtins.eclipse;

import java.io.File;

import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.builtins.eclipse.DotClasspath.ClasspathEntry;
import org.jerkar.tool.builtins.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

class ScopeResolverSmart implements ScopeResolver {

    private final WstCommonComponent wstCommonComponent;

    public ScopeResolverSmart(WstCommonComponent wstCommonComponent) {
	super();
	this.wstCommonComponent = wstCommonComponent;
    }

    @Override
    public JkScope scopeOfLib(Kind kind, String path) {
	JkScope scope = scopeOfLibAccordingLocation(new File(path));
	if (wstCommonComponent != null) {
	    final ClasspathEntry classpathEntry = ClasspathEntry.of(kind, path);
	    if (!wstCommonComponent.contains(classpathEntry)) {
		if (scope.isInOrIsExtendingAnyOf(JkJavaBuild.COMPILE)) {
		    JkLog.trace(path + " not found as module in " + WstCommonComponent.FILE
			    + " : turn scope to 'provided'.");
		    scope = JkJavaBuild.PROVIDED;
		}
	    }
	}
	return scope;
    }

    private JkScope scopeOfLibAccordingLocation(File libFile) {
	final String parent = libFile.getParentFile().getName();
	if (parent.equalsIgnoreCase(JkJavaBuild.COMPILE.name())) {
	    return JkJavaBuild.COMPILE;
	}
	if (parent.equalsIgnoreCase(JkJavaBuild.TEST.name())) {
	    return JkJavaBuild.TEST;
	}
	if (parent.equalsIgnoreCase(JkJavaBuild.PROVIDED.name())) {
	    return JkJavaBuild.PROVIDED;
	}
	if (parent.equalsIgnoreCase(JkJavaBuild.RUNTIME.name())) {
	    return JkJavaBuild.RUNTIME;
	}
	final String path = libFile.getPath();
	final String name = path.contains("/") ? JkUtilsString.substringAfterLast(path, "/") : path;
	if (name.toLowerCase().contains("junit")) {
	    return JkJavaBuild.TEST;
	}
	if (name.toLowerCase().contains("lombok")) {
	    return JkJavaBuild.PROVIDED;
	}
	return JkJavaBuild.COMPILE;
    }

    @Override
    public JkScope scopeOfCon(String path) {
	if (path.contains("org.eclipse.jdt.junit.JUNIT_CONTAINER")) {
	    return JkJavaBuild.TEST;
	}
	return JkJavaBuild.COMPILE;
    }

}
