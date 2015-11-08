package org.jerkar.api.tooling;

import java.io.File;
import java.net.URL;

import org.junit.Test;

public class EffectivePomTest {

    @Test
    public void test() {
	final URL url = EffectivePomTest.class.getResource("effectivepom.xml");
	final File file = new File(url.getFile());
	final EffectivePom effectivePom = EffectivePom.of(file);
	effectivePom.dependencies();
	effectivePom.artifactId();
	effectivePom.dependencyExclusion();
	effectivePom.repos();
    }

}
