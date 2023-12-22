package dev.jeka.core.tool.builtins.maven;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkMvn;
import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Files;
import java.nio.file.Path;

import static dev.jeka.core.api.depmanagement.JkQualifiedDependencySet.*;

@JkDoc("Provides method to help migration from Maven.")
public class MavenKBean extends KBean {

    @JkDoc("whitespace count to indentSpri dependency code.")
    public int codeIndent = 4;

    @JkDoc("Displays Java code for declaring dependencies based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void migrateToCode()  {
        if (!Files.exists(getBaseDir().resolve("pom.xml"))) {
            throw new IllegalStateException(("No pom.xml file found at " + JkUtilsPath.friendlyName(getBaseDir())
                    + ". Won't process."));
        }
        Path effectivePom = JkUtilsPath.createTempFile("jeka-effective-pom-", ".pom");
        JkMvn.of(getBaseDir(), "help:effective-pom", "-Doutput=" + effectivePom.toString())
                .toProcess()
                .setLogCommand(true)
                .setLogOutput(JkLog.isVerbose())
                .run();
        //Path pomPath = getBaseDir().resolve("pom.xml");
        //JkUtilsAssert.state(Files.exists(pomPath), "No pom file found at " + pomPath);
        JkPom pom = JkPom.of(effectivePom);
        System.out.println("Compile");
        System.out.println(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(null,
                COMPILE_SCOPE, PROVIDED_SCOPE), true));

        System.out.println("Runtime");
        System.out.print(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(
                RUNTIME_SCOPE), true));
        System.out.println(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(
                PROVIDED_SCOPE), false));

        System.out.println("Test");
        System.out.println(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(
                TEST_SCOPE), true));
    }

    @JkDoc("Displays project-dependencies content based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void migrateToDependenciesTxt()  {
        if (!Files.exists(getBaseDir().resolve("pom.xml"))) {
            throw new IllegalStateException(("No pom.xml file found at " + JkUtilsPath.friendlyName(getBaseDir())
                    + ". Won't process."));
        }
        Path effectivePom = JkUtilsPath.createTempFile("jeka-effective-pom-", ".pom");
        JkMvn.of(getBaseDir(), "help:effective-pom", "-Doutput=" + effectivePom.toString())
                .toProcess()
                .setLogCommand(true)
                .setLogOutput(JkLog.isVerbose())
                .run();
        JkPom pom = JkPom.of(effectivePom);
        System.out.println("\n==== COMPILE ====");
        System.out.print(JkDependencySet.toTxt( pom.getDependencies().getDependenciesHavingQualifier(null,
                COMPILE_SCOPE, PROVIDED_SCOPE), false));

        System.out.println("\n\n==== RUNTIME ====");
        System.out.print(JkDependencySet.toTxt(pom.getDependencies().getDependenciesHavingQualifier(
                RUNTIME_SCOPE), false));
        System.out.print(JkDependencySet.toTxt(pom.getDependencies().getDependenciesHavingQualifier(
                PROVIDED_SCOPE), true));

        System.out.println("\n\n==== TEST ====");
        System.out.println(JkDependencySet.toTxt(pom.getDependencies().getDependenciesHavingQualifier(
                TEST_SCOPE), false));


    }
}
