package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.project.JkProject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class JkComputedDependencyTest {

    @Test
    public void onProject() throws IOException {
        Path root = Files.createTempDirectory("jekatestproject");
        JkProject javaProject = JkProject.of()
            .setBaseDir(root);
        JkDependencySet dependencies = JkDependencySet.of().and(javaProject.toDependency());
        JkComputedDependency computedDependency = (JkComputedDependency) dependencies.getEntries().get(0);
        Assert.assertEquals(root, computedDependency.getIdeProjectDir().toAbsolutePath().normalize());
    }

}
