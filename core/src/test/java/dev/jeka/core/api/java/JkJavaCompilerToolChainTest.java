package dev.jeka.core.api.java;


import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkJavaCompilerToolChainTest {

    @Test
    public void currentJdkSourceVersion() {
        assertEquals("12", JkJavaCompilerToolChain.runningJdkVersion("12.0.1"));
        assertEquals("8", JkJavaCompilerToolChain.runningJdkVersion("1.8.0_211"));
        assertEquals("9", JkJavaCompilerToolChain.runningJdkVersion("9.0.1"));
    }

    @Test
    void compile_forkedOnJdk8_noModuleExportOptions() throws IOException {
        Assumptions.assumeFalse(JkUtilsSystem.IS_WINDOWS);
        assertFalse(forkedJavacArgs("8").contains("--add-exports"));
        assertTrue(forkedJavacArgs("17").contains("--add-exports"));  // still there for JDKs knowing modules
    }

    @Test
    void compile_jdkDownloadFails_embeddedCompilerTargetsRequestedVersion() throws IOException {
        Path dir = Files.createTempDirectory("jk-jdk-fallback");
        Path srcDir = Files.createDirectories(dir.resolve("src"));
        Files.writeString(srcDir.resolve("A.java"), "class A {}");
        JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of()
                .setSources(JkPathTree.of(srcDir).toSet())
                .setOutputDir(dir.resolve("classes"));

        JkJavaCompilerToolChain.Status status = JkJavaCompilerToolChain.of()
                .setJavaDistrib("jeka-test-unknown-distrib")  // the JDK download fails
                .compile(JkJavaVersion.of("11"), compileSpec);

        assertEquals(JkJavaCompilerToolChain.Status.SUCCESS, status);
        byte[] classBytes = Files.readAllBytes(dir.resolve("classes/A.class"));
        assertEquals(55, ((classBytes[6] & 0xff) << 8) | (classBytes[7] & 0xff));  // Java 11
        JkPathTree.of(dir).deleteRoot();
    }

    // Returns the arguments passed to a fake javac of the specified version
    private static String forkedJavacArgs(String jdkVersion) throws IOException {
        Path dir = Files.createTempDirectory("jk-fake-jdk");
        Path argsFile = dir.resolve("args.txt");
        Path javac = Files.createDirectories(dir.resolve("jdk/bin")).resolve("javac");
        Files.writeString(javac, "#!/bin/sh\necho \"$@\" > '" + argsFile + "'\n");
        javac.toFile().setExecutable(true);
        Path srcDir = Files.createDirectories(dir.resolve("src"));
        Files.writeString(srcDir.resolve("A.java"), "class A {}");

        JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of()
                .setSources(JkPathTree.of(srcDir).toSet())
                .setOutputDir(dir.resolve("classes"));
        JkJavaCompilerToolChain.of()
                .setJdkHints(JkJavaCompilerToolChain.JkJdks.ofJdkHomeProps(
                        Map.of(jdkVersion, dir.resolve("jdk").toString())), false)
                .compile(JkJavaVersion.of(jdkVersion), compileSpec);
        String result = Files.readString(argsFile);
        JkPathTree.of(dir).deleteRoot();
        return result;
    }
}
