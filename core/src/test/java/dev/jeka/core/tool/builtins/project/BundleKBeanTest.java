package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.tools.JkJpackage;
import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleKBeanTest {

    @TempDir
    Path tempDir;

    @Test
    void pack_customJreNoDeclaredModules_modulesAreComputed() throws Exception {
        Path baseDir = tempDir.toRealPath();
        String release = packAndReadRelease(baseDir, "");
        assertTrue(release.contains("java.logging"), release);
        assertFalse(release.contains("java.sql"), release);
    }

    @Test
    void pack_customJreModulesSetByUser_modulesAreKept() throws Exception {
        Path baseDir = tempDir.toRealPath();
        String release = packAndReadRelease(baseDir,
                "@bundle.jlink.options.all.--add-modules=java.base,java.xml\n");
        assertTrue(release.contains("java.xml"), release);
        assertFalse(release.contains("java.logging"), release);
    }

    @Test
    void jdkHome_jdkSetInProperties_isUsed() throws Exception {
        Path baseDir = tempDir.toRealPath();
        Path jdkHome = Files.createDirectories(baseDir.resolve("my-jdk"));
        String javaVersion = JkJavaVersion.ofCurrent().toString();
        write(baseDir.resolve("jeka.properties"), "@project=on\n@bundle=on\njeka.jdk." + javaVersion + "="
                + jdkHome.toString().replace("\\", "/") + "\n");
        assertEquals(jdkHome, KBeanTestLoader.load(baseDir, BundleKBean.class).jdkHome());
    }

    @Test
    void selectForOs_eachOs_returnsItsValue() {
        assertEquals("win", BundleKBean.selectForOs(true, false, "win", "mac", "linux"));
        assertEquals("mac", BundleKBean.selectForOs(false, true, "win", "mac", "linux"));
        assertEquals("linux", BundleKBean.selectForOs(false, false, "win", "mac", "linux"));
    }

    @Test
    void jlinkProjectOptions_projectHasModulePath_passesIt() throws Exception {
        Path baseDir = tempDir.toRealPath();
        Path modDir = Files.createDirectories(baseDir.resolve("mods"));
        write(baseDir.resolve("jeka.properties"), "@project=on\n@bundle=on\n@bundle.customJre=true\n"
                + "@bundle.jlink.options.all.--add-modules=java.base\n");
        BundleKBean bundle = KBeanTestLoader.load(baseDir, BundleKBean.class);
        bundle.getRunbase().load(ProjectKBean.class).project.jpmsModules.modulePathCustomizer
                .append(paths -> paths.add(modDir));

        List<String[]> options = bundle.jlinkProjectOptions();

        String[] modulePath = options.stream().filter(o -> o[0].equals("--module-path")).findFirst().orElseThrow();
        assertTrue(modulePath[1].contains(modDir.toString()), Arrays.toString(modulePath));
        assertFalse(options.stream().anyMatch(o -> o[0].equals("--add-modules")), "set by user, not computed");
    }

    @Test
    void setProjectOptions_noCustomJreWithModulePathAndJdeps_addsBoth() throws Exception {
        Path baseDir = tempDir.toRealPath();
        Path modDir = Files.createDirectories(baseDir.resolve("mods"));
        write(baseDir.resolve("jeka.properties"), "@project=on\n@project.pack.mainClass=app.Main\n@bundle=on\n"
                + "@bundle.jpackage.options.all.--add-modules=JDEPS-COMPUTE\n");
        write(baseDir.resolve("src/main/java/app/Main.java"),
                "package app; public class Main { public static void main(String[] args) { "
                        + "java.util.logging.Logger.getGlobal(); } }");
        BundleKBean bundle = KBeanTestLoader.load(baseDir, BundleKBean.class);
        ProjectKBean projectKBean = bundle.getRunbase().load(ProjectKBean.class);
        projectKBean.project.jpmsModules.modulePathCustomizer.append(paths -> paths.add(modDir));
        projectKBean.project.pack.run();
        JkJpackage jpackage = JkJpackage.of();

        bundle.setProjectOptions(jpackage, projectKBean.project.artifactLocator.getMainArtifactPath());

        List<String> modulePath = jpackage.findOptionValues("--module-path").orElseThrow();
        assertTrue(modulePath.get(0).contains(modDir.toString()), modulePath.toString());
        List<String> addModules = jpackage.findOptionValues("--add-modules").orElseThrow();
        assertTrue(addModules.get(0).contains("java.logging"), addModules.toString());
    }

    private static String packAndReadRelease(Path baseDir, String extraProps) throws Exception {
        write(baseDir.resolve("jeka.properties"),
                "@project=on\n@project.pack.mainClass=app.Main\n@bundle=on\n@bundle.customJre=true\n"
                        + "@bundle.jpackage.options.all.--type=app-image\n" + extraProps);
        write(baseDir.resolve("src/main/java/app/Main.java"),
                "package app; public class Main { public static void main(String[] args) { "
                        + "java.util.logging.Logger.getGlobal(); } }");
        KBeanTestLoader.load(baseDir, BundleKBean.class).pack();
        return new String(Files.readAllBytes(baseDir.resolve("jeka-output/jlink/custom-jre/release")));
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }

}
