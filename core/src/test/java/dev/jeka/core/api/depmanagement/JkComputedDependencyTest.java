package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.project.JkProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


class JkComputedDependencyTest {

    @Test
    void onProject() throws IOException {
        Path root = Files.createTempDirectory("jekatestproject");
        JkProject javaProject = JkProject.of()
            .setBaseDir(root);
        JkDependencySet dependencies = JkDependencySet.of().and(javaProject.toDependency());
        JkComputedDependency computedDependency = (JkComputedDependency) dependencies.getEntries().get(0);
        Assertions.assertEquals(root, computedDependency.getIdeProjectDir().toAbsolutePath().normalize());
    }

}
