package org.jerkar.api.tooling;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Assert;
import org.junit.Test;

public class EffectivePomTest {

    @Test
    public void test() {
        final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
        final File file = new File(url.getFile());
        final JkPom jkPom = JkPom.of(file);
        jkPom.dependencies();
        jkPom.artifactId();
        jkPom.dependencyExclusion();
        jkPom.repos();
    }

    @Test
    public void testJerkarSourceCode() throws IOException {
        final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
        final File file = new File(url.getFile());
        final JkPom jkPom = JkPom.of(file);
        final String code = jkPom.jerkarSourceCode();
        System.out.println(code);
        final File srcDir = new File("build/output/test-generated-src");
        srcDir.mkdirs();
        final File binDir = new File("build/output/test-generated-bin");
        binDir.mkdirs();
        final File javaCode = new File(srcDir, "Build.java");
        javaCode.getParentFile().mkdirs();
        javaCode.createNewFile();
        JkUtilsFile.writeString(javaCode, code, false);
        final boolean success = JkJavaCompiler.ofOutput(binDir).andSourceDir(srcDir).compile();
        Assert.assertTrue("The generated build class does not compile " + javaCode, success);
    }

}
