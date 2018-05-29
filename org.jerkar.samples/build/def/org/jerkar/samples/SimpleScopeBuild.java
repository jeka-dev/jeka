package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.RUNTIME;
import static org.jerkar.api.depmanagement.JkPopularModules.JERSEY_SERVER;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * This build illustrates how one can use other dependency scope mapping then the standard ones.
 */
public class SimpleScopeBuild extends JkJavaProjectBuild {

    private static final JkScope FOO = JkScope.of("foo");

    private static final JkScope BAR = JkScope.of("bar");

    protected JkJavaProject createProject() {
        return defaultProject().setDependencies(JkDependencies.builder()
                .on(baseDir().resolve("libs/foo.jar"))
                .on(JERSEY_SERVER, "1.19")
                    .mapScope(COMPILE).to(RUNTIME)
                    .and(FOO, PROVIDED).to(BAR, PROVIDED).build());
    }
}


