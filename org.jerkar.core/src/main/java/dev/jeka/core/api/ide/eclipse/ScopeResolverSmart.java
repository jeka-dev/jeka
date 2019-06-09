package dev.jeka.core.api.ide.eclipse;

import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;

class ScopeResolverSmart implements ScopeResolver {

    private final WstCommonComponent wstCommonComponent;

    public ScopeResolverSmart(WstCommonComponent wstCommonComponent) {
        super();
        this.wstCommonComponent = wstCommonComponent;
    }

    @Override
    public JkScope scopeOfLib(DotClasspathModel.ClasspathEntry.Kind kind, String path) {
        JkScope scope = scopeOfLibAccordingLocation(Paths.get(path));
        if (wstCommonComponent != null) {
            final DotClasspathModel.ClasspathEntry classpathEntry = DotClasspathModel.ClasspathEntry.of(kind, path);
            if (!wstCommonComponent.contains(classpathEntry)) {
                if (scope.isInOrIsExtendingAnyOf(COMPILE)) {
                    JkLog.trace(path + " not found as module in " + WstCommonComponent.FILE
                            + " : turn scope to 'provided'.");
                    scope = PROVIDED;
                }
            }
        }
        return scope;
    }

    private JkScope scopeOfLibAccordingLocation(Path libFile) {
        final String parent = libFile.getParent().getFileName().toString();
        if (parent.equalsIgnoreCase(COMPILE.getName())) {
            return COMPILE;
        }
        if (parent.equalsIgnoreCase(TEST.getName())) {
            return TEST;
        }
        if (parent.equalsIgnoreCase(PROVIDED.getName())) {
            return PROVIDED;
        }
        if (parent.equalsIgnoreCase(RUNTIME.getName())) {
            return RUNTIME;
        }
        final String path = libFile.toString();
        final String name = path.contains("/") ? JkUtilsString.substringAfterLast(path, "/") : path;
        if (name.toLowerCase().contains("junit")) {
            return TEST;
        }
        if (name.toLowerCase().contains("lombok")) {
            return PROVIDED;
        }
        return COMPILE;
    }

    @Override
    public JkScope scopeOfCon(String path) {
        if (path.contains("org.eclipse.jdt.junit.JUNIT_CONTAINER")) {
            return TEST;
        }
        return COMPILE;
    }

}
