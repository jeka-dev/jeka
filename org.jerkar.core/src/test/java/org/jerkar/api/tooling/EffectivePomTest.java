package org.jerkar.api.tooling;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.java.JkJavaCompileSpec;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class EffectivePomTest {

    @Test
    public void test() throws URISyntaxException {
        final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
        final Path file = Paths.get(url.toURI());
        final JkPom jkPom = JkPom.of(file);
        jkPom.dependencies();
        jkPom.artifactId();
        jkPom.dependencyExclusion();
        jkPom.repos();
    }

    // Compilation fails for obscure reason (stack overflow)
    public void testJerkarSourceCode() throws IOException, URISyntaxException {
        final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
        final Path file = Paths.get(url.toURI());
        final JkPom jkPom = JkPom.of(file);
        final String code = jkPom.jerkarSourceCode(JkFileTree.of(new File("toto")));
        System.out.println(code);
        final Path srcDir = Paths.get("build/output/test-generated-src");
        Files.createDirectories(srcDir);
        final Path binDir = Paths.get("build/output/test-generated-bin");
        Files.createDirectories(binDir);
        final File javaCode = new File(srcDir.toFile(), "Build.java");
        javaCode.getParentFile().mkdirs();
        javaCode.createNewFile();
        JkUtilsFile.writeString(javaCode, code, false);
        final boolean success = JkJavaCompiler.base().compile( new JkJavaCompileSpec()
                .setOutputDir(binDir)
                .addSources(srcDir)
                .setOption("-cp", JkLocator.jerkarJarPath().toAbsolutePath().normalize().toString()));
        Assert.assertTrue("The generated build class does not compile " + javaCode, success);
    }

}
