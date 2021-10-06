package dev.jeka.core.api.tooling;

import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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
