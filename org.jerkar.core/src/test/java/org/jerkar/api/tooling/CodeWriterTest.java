package org.jerkar.api.tooling;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Assert;
import org.junit.Test;

public class CodeWriterTest {

    public static void main(String[] args) {
	final URL url = CodeWriterTest.class.getResource("effectivepom.xml");
	final File file = new File(url.getFile());
	final EffectivePom effectivePom = EffectivePom.of(file);
	final CodeWriter codeWriter = new CodeWriter(effectivePom);
	System.out.println(codeWriter.wholeClass("my.packagename", "MyBuildClass"));
    }

    @Test
    public void testCompile() throws IOException {
	final URL url = CodeWriterTest.class.getResource("effectivepom.xml");
	final File file = new File(url.getFile());
	final EffectivePom effectivePom = EffectivePom.of(file);
	final CodeWriter codeWriter = new CodeWriter(effectivePom);
	final File srcDir = new File("build/output/test-generated-src");
	srcDir.mkdirs();
	final File binDir = new File("build/output/test-generated-bin");
	binDir.mkdirs();
	final File javaCode = new File(srcDir, "my/packagename/MyBuildClass.java");
	javaCode.getParentFile().mkdirs();
	javaCode.createNewFile();
	JkUtilsFile.writeString(javaCode, codeWriter.wholeClass("my.packagename", "MyBuildClass"), false);
	final boolean success = JkJavaCompiler.ofOutput(binDir).andSourceDir(srcDir).compile();
	Assert.assertTrue("The generated build class does not compile " + javaCode, success);
    }


}
