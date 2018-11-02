package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.RUNTIME;
import static org.jerkar.api.depmanagement.JkPopularModules.JERSEY_SERVER;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * This build illustrates how one can use other dependency scope mapping then the standard ones.
 */
public class SimpleScopeBuild extends JkJavaProjectBuild {

    private static final JkScope FOO = JkScope.of("foo");

    private static final JkScope BAR = JkScope.of("bar");

    @Override
    protected void setup() {
        java().project().setDependencies(JkDependencySet.of()
                .andFile(getBaseDir().resolve("libs/foo.jar"))
                .and(JERSEY_SERVER, "1.19", JkScopeMapping
                    .of(COMPILE).to(RUNTIME)
                    .and(FOO, PROVIDED).to(BAR, PROVIDED)));
        BuildUtility.printHello();
    }


}


