package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.depmanagement.JkScopeMapping;
import dev.jeka.core.tool.JkRun;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;
import static dev.jeka.core.api.depmanagement.JkPopularModules.JERSEY_SERVER;

/**
 * This build illustrates how one can use other dependency scope mapping then the standard ones.
 */
public class SimpleScopeBuild extends JkRun {

    private static final JkScope FOO = JkScope.of("foo");

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    @Override
    protected void setup() {
        javaPlugin.getProject().addDependencies(JkDependencySet.of()
                .andFile(getBaseDir().resolve("libs/foo.jar"))
                .and(JERSEY_SERVER, "1.19", JkScopeMapping
                    .of(COMPILE).to(RUNTIME.getName())
                    .and(FOO, PROVIDED).to("bar", PROVIDED.getName())));
        BuildUtility.printHello();
    }


}


