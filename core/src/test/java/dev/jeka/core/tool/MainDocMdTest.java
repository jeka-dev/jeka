package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainDocMdTest {

    @TempDir
    Path baseDir;

    private BehaviorSettings initialSettings;

    private ClassLoader initialClassloader;

    private PrintStream initialOut;

    @BeforeEach
    void setUp() {
        initialSettings = BehaviorSettings.INSTANCE;
        initialClassloader = Thread.currentThread().getContextClassLoader();
        initialOut = System.out;
        BehaviorSettings.INSTANCE = BehaviorSettings.ofDefault();
    }

    @AfterEach
    void tearDown() {
        BehaviorSettings.INSTANCE = initialSettings;
        Thread.currentThread().setContextClassLoader(initialClassloader);
        System.setOut(initialOut);
    }

    @Test
    void performDocMdKBean_standardKBean_printsMarkdown() throws Exception {
        Engine engine = Engine.of(true, baseDir.toRealPath(), JkRepoSet.ofLocal(), JkDependencySet.of());
        engine.resolveKBeans();
        Engines.registerMaster(engine);
        Thread.currentThread().setContextClassLoader(
                new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
        engine.getOrCreateRunbase(new KBeanAction.Container(), true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true));

        assertTrue(Main.performDocMdKBean(engine, "base"));
        assertTrue(out.toString().contains("jarType"), out.toString());
        assertFalse(Main.performDocMdKBean(engine, ""));
    }

}
