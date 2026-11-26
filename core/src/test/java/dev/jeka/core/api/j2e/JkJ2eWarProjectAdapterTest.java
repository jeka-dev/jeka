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

    @Test
    void generateExplodedWar_twice_doesNotKeepDeletedClasses() throws Exception {
        Path base = Files.createTempDirectory("jk-war-exploded");
        Path srcDir = Files.createDirectories(base.resolve("src/main/java/app"));
        Files.writeString(srcDir.resolve("App.java"), "package app; public class App {}");
        Path deleted = srcDir.resolve("Deleted.java");
        Files.writeString(deleted, "package app; public class Deleted {}");
        Path war = base.resolve("app.war");
        JkJ2eWarProjectAdapter.of().setGenerateExploded(true).generateWar(JkProject.of().setBaseDir(base), war);

        Files.delete(deleted);
        JkProject project = JkProject.of().setBaseDir(base);   // a new project, as for a second 'jeka' run
        JkJ2eWarProjectAdapter.of().setGenerateExploded(true).generateWar(project, war);

        Path explodedDir = project.getOutputDir().resolve("j2e-war");
        Assertions.assertTrue(Files.exists(explodedDir.resolve("WEB-INF/classes/app/App.class")));
        Assertions.assertFalse(Files.exists(explodedDir.resolve("WEB-INF/classes/app/Deleted.class")));
        try (JkZipTree zipTree = JkZipTree.of(war)) {
            Assertions.assertFalse(Files.exists(zipTree.get("WEB-INF/classes/app/Deleted.class")));
        }
        JkPathTree.of(base).deleteRoot();
    }

}
