package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the '-cb' option in-process. See also e2e.ChildBaseOptionTest, which runs the jeka command.
 */
class ChildBaseFilterTest {

    @TempDir
    Path baseDir;

    private BehaviorSettings initialSettings;

    private ClassLoader initialClassloader;

    private JkRunbase runbase;

    @BeforeEach
    void setUp() throws IOException {
        initialSettings = BehaviorSettings.INSTANCE;
        initialClassloader = Thread.currentThread().getContextClassLoader();
        BehaviorSettings.INSTANCE = BehaviorSettings.ofDefault();

        Path dir = baseDir.toRealPath();
        write(dir.resolve("jeka.properties"), "_jeka.child-bases=mods/a\n");
        write(dir.resolve("mods/a/jeka.properties"), "");
        Engine engine = Engine.of(true, dir, JkRepoSet.ofLocal(), JkDependencySet.of());
        engine.resolveKBeans();
        Engines.registerMaster(engine);
        Thread.currentThread().setContextClassLoader(
                new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
        runbase = engine.getOrCreateRunbase(new KBeanAction.Container(), true);
    }

    @AfterEach
    void tearDown() {
        BehaviorSettings.INSTANCE = initialSettings;
        Thread.currentThread().setContextClassLoader(initialClassloader);
    }

    @Test
    void undeclaredChildBase_fails() {
        setChildBase("mods/b", false);
        JkException e = assertThrows(JkException.class, this::run);
        assertTrue(e.getMessage().contains("No child base found at location mods/b"), e.getMessage());
        assertTrue(e.getMessage().contains("[mods/a]"), e.getMessage());
        assertTrue(e.getMessage().contains("--force"), e.getMessage());
    }

    @Test
    void undeclaredChildBase_withForce_onlyWarns() {
        setChildBase("mods/b", true);
        assertDoesNotThrow(this::run);
    }

    @Test
    void declaredChildBase_anySeparator_ok() {
        for (String value : new String[] {"mods/a", "mods/a/", " mods\\a ", "mods\\a\\", "mods/a//"}) {
            setChildBase(value, false);
            assertDoesNotThrow(this::run, value);
        }
    }

    @Test
    void currentBase_ok() {
        setChildBase(".", false);
        assertDoesNotThrow(this::run);
    }

    private void run() {
        runbase.run(new KBeanAction.Container(), true);
    }

    private static void setChildBase(String childBase, boolean force) {
        BehaviorSettings.INSTANCE = new BehaviorSettings(null, false, false, force, false, childBase, false, false);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }

}
