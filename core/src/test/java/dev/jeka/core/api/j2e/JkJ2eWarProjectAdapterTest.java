package dev.jeka.core.api.j2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.project.JkProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

class JkJ2eWarProjectAdapterTest {

    @Test
    void generateWar_doesNotRunTests() throws Exception {
        Path base = Files.createTempDirectory("jk-war-project");
        Path srcDir = Files.createDirectories(base.resolve("src/main/java/app"));
        Files.writeString(srcDir.resolve("App.java"), "package app; public class App {}");
        JkProject project = JkProject.of().setBaseDir(base);
        AtomicBoolean testRun = new AtomicBoolean();
        project.test.compilation.preCompileActions.append("flag", () -> testRun.set(true));
        Path war = base.resolve("app.war");

        JkJ2eWarProjectAdapter.of().generateWar(project, war);

        Assertions.assertFalse(testRun.get(), "Packaging a war should not run tests");
        try (JkZipTree zipTree = JkZipTree.of(war)) {
            Assertions.assertTrue(Files.exists(zipTree.get("WEB-INF/classes/app/App.class")));
        }
        JkPathTree.of(base).deleteRoot();
    }

}
