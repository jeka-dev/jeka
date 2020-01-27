package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.depmanagement.JkScopeMapping;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;

/**
 * This build illustrates how one can use other dependency scope mapping then the standard ones.
 */
public class SimpleScopeBuild extends JkCommands {

    private static final JkScope FOO = JkScope.of("foo");

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    @Override
    protected void setup() {
        javaPlugin.getProject().addDependencies(JkDependencySet.of()
                .andFile(getBaseDir().resolve("libs/foo.jar"))
                .and("junit:junit:4.11", TEST)
                .and("com.sun.jersey:jersey-server:1.19", JkScopeMapping
                        .of(COMPILE).to(RUNTIME.getName())));

        BuildUtility.printHello();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SimpleScopeBuild.class).javaPlugin.pack();
    }


}


