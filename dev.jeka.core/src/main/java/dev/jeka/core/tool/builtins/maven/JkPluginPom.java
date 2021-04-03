package dev.jeka.core.tool.builtins.maven;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDoc("Provides method to help migration from Maven.")
public class JkPluginPom extends JkPlugin {

    protected JkPluginPom(JkClass jkClass) {
        super(jkClass);
    }

    @JkDoc("Margin to print dependency code.")
    public int margin = 6;

    @JkDoc("Displays Java code for declaring dependencies on console based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void dependencyCode() {

        Path pomPath = getJkClass().getBaseDir().resolve("pom.xml");
        JkUtilsAssert.state(Files.exists(pomPath), "No pom file found at " + pomPath);
        JkPom pom = JkPom.of(pomPath);
        System.out.println("Compile");
        System.out.println(JkDependencySet.toJavaCode(margin, pom.getDependencies().getDependenciesHavingQualifier(null,
                JkQualifiedDependencies.COMPILE_SCOPE, JkQualifiedDependencies.PROVIDED_SCOPE)));
        System.out.println(".withVersionProvider(" + pom.getVersionProvider().toJavaCode(margin) + ")");

        System.out.println("\nRuntime");
        System.out.println(JkDependencySet.toJavaCode(margin, pom.getDependencies().getDependenciesHavingQualifier(
                JkQualifiedDependencies.RUNTIME_SCOPE)));
        System.out.println("\nTest");
        System.out.println(JkDependencySet.toJavaCode(margin, pom.getDependencies().getDependenciesHavingQualifier(
                JkQualifiedDependencies.TEST_SCOPE)));
    }
}
