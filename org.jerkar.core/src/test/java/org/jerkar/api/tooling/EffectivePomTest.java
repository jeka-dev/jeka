package org.jerkar.api.tooling;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class EffectivePomTest {

    @Test
    public void test() throws URISyntaxException {
        final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
        final Path file = Paths.get(url.toURI());
        final JkPom jkPom = JkPom.of(file);
        jkPom.getDependencies();
        jkPom.getArtifactId();
        jkPom.getDependencyExclusion();
        jkPom.getRepos();
    }


}
