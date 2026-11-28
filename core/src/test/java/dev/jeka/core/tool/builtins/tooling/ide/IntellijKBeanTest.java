package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.tooling.intellij.JkIntellijIml;
import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntellijKBeanTest {

    @Test
    void sync_jdkDownloadFails_imlGenerated(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.resolve("my-project");
        Files.createDirectories(baseDir.resolve("src/main/java/app"));
        Files.writeString(baseDir.resolve("src/main/java/app/A.java"), "package app; public class A {}\n");
        Files.createDirectories(baseDir.resolve(".idea"));
        Files.writeString(baseDir.resolve(".idea/workspace.xml"), "");  // IntelliJ project root, for modules.xml

        // No JDK of this distribution can be downloaded
        Files.writeString(baseDir.resolve("jeka.properties"), "@project=on\n@project.javaVersion=8\n"
                + "jeka.java.distrib=jk-test-unknown-distrib\n");
        IntellijKBean intellijKBean = KBeanTestLoader.load(baseDir.toRealPath(), IntellijKBean.class);

        intellijKBean.sync();

        String iml = Files.readString(baseDir.resolve(".idea/my-project.iml"));
        assertTrue(iml.contains("jeka-jk-test-unknown-distrib-8"), iml);
    }

    @Test
    void sync_singleIml_keepsSdkAndAppliesCustomizers(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.resolve("my-project");
        Files.createDirectories(baseDir.resolve("src/main/java/app"));
        Files.writeString(baseDir.resolve("src/main/java/app/A.java"), "package app; public class A {}\n");
        Files.createDirectories(baseDir.resolve(".idea"));
        Files.writeString(baseDir.resolve(".idea/workspace.xml"), "");  // IntelliJ project root, for modules.xml

        // SDK chosen by the user in IntelliJ: sync keeps it
        Path imlFile = baseDir.resolve(".idea/my-project.iml");
        Files.writeString(imlFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="JAVA_MODULE" version="4">
                  <component name="NewModuleRootManager" inherit-compiler-output="true">
                    <orderEntry type="jdk" jdkName="jk-test-user-sdk" jdkType="JavaSDK" />
                  </component>
                </module>
                """);

        // No 'javaVersion': the project runs on the running JDK, so a single iml is generated
        Files.writeString(baseDir.resolve("jeka.properties"), "@project=on\n");
        IntellijKBean intellijKBean = KBeanTestLoader.load(baseDir.toRealPath(), IntellijKBean.class);
        intellijKBean.customizeIml(iml -> iml.component.addModuleOrderEntry("jk-test-module",
                JkIntellijIml.Scope.COMPILE));

        intellijKBean.sync();

        String iml = Files.readString(imlFile);
        assertTrue(iml.contains("jk-test-module"), iml);
        assertTrue(iml.contains("jk-test-user-sdk"), iml);
        assertFalse(Files.exists(baseDir.resolve(".idea/my-project-jeka.iml")), "single iml expected");
    }

}
