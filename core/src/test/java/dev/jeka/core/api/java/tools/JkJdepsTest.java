package dev.jeka.core.api.java.tools;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkJavaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkJdepsTest {

    @Test
    void getModuleDeps_multiReleaseJar_versionedClassesAnalyzed(@TempDir Path dir) throws Exception {
        String version = Integer.toString(JkJavaVersion.ofCurrent().get());
        Path baseClass = compile(dir.resolve("base"), "public class A {}");
        Path versionedClass = compile(dir.resolve("versioned"),
                "public class A { java.net.http.HttpClient client; }");
        Path jar = dir.resolve("multi-release.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Multi-Release"), "true");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            put(out, "A.class", baseClass);
            put(out, "META-INF/versions/" + version + "/A.class", versionedClass);
        }

        List<String> versionedDeps = JkJdeps.of().setMultiRelease(version).getModuleDeps(jar, JkPathSequence.of());
        List<String> baseDeps = JkJdeps.of().getModuleDeps(jar, JkPathSequence.of());

        assertTrue(versionedDeps.contains("java.net.http"), versionedDeps.toString());
        assertFalse(baseDeps.contains("java.net.http"), baseDeps.toString());
    }

    private static Path compile(Path dir, String code) throws IOException {
        Path source = Files.createDirectories(dir).resolve("A.java");
        Files.writeString(source, code);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", dir.toString(), source.toString());
        assertTrue(result == 0, "Compilation failed");
        return dir.resolve("A.class");
    }

    private static void put(JarOutputStream out, String name, Path file) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(Files.readAllBytes(file));
        out.closeEntry();
    }

}
