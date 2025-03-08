package dev.jeka.core.api.tooling;

import dev.jeka.core.api.tooling.maven.JkPom;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

class EffectivePomTest {

    @Test
    void test() throws URISyntaxException {
        final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
        final Path file = Paths.get(url.toURI());
        final dev.jeka.core.api.tooling.maven.JkPom jkPom = JkPom.of(file);
        jkPom.getDependencies();
        jkPom.getArtifactId();
        jkPom.getDependencyExclusion();
        jkPom.getRepos();
    }


}
