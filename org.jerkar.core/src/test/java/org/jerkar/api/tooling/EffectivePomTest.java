package org.jerkar.api.tooling;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.file.JkPathFile;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaCompileSpec;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.system.JkLocator;
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


}
