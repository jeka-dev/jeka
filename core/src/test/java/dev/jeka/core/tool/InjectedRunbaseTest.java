package dev.jeka.core.tool;

import dev.jeka.core.tool.builtins.base.BaseKBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;

import static org.junit.jupiter.api.Assertions.*;

class InjectedRunbaseTest {

    @TempDir
    Path baseDir;

    private BehaviorSettings initialSettings;

    private ClassLoader initialClassloader;

    @BeforeEach
    void setUp() {
        initialSettings = BehaviorSettings.INSTANCE;
        initialClassloader = Thread.currentThread().getContextClassLoader();
        BehaviorSettings.INSTANCE = BehaviorSettings.ofDefault();
    }

    @AfterEach
    void tearDown() {
        BehaviorSettings.INSTANCE = initialSettings;
        Thread.currentThread().setContextClassLoader(initialClassloader);
    }

    @Test
    void childInjectingSibling_fromParent_ok() throws IOException {
        Path dir = baseDir.toRealPath();
        write(dir.resolve("jeka.properties"), "_jeka.child-bases=b, a\n");
        write(dir.resolve("a/jeka.properties"), "@project=on\n");
        write(dir.resolve("b/jeka.properties"), "@project=on\n");
        write(dir.resolve("b/jeka-src/BuildB.java"),
                "import dev.jeka.core.tool.*;\n" +
                "import dev.jeka.core.tool.builtins.project.ProjectKBean;\n" +
                "class BuildB extends KBean {\n" +
                "  @JkInject(\"../a\") ProjectKBean a;\n" +
                "  @JkInject(\"../a\") JkRunbase aBase;\n" +
                "  public void init() { System.setProperty(\"injected.base\", a.getBaseDir().toString());\n" +
                "    System.setProperty(\"injected.runbase\", aBase.getBaseDir().toString()); }\n" +
                "}\n");
        try {
            Engine engine = Engine.of(true, dir, JkRepoSet.ofLocal(), JkDependencySet.of());
            engine.resolveKBeans();
            Engines.registerMaster(engine);
            Thread.currentThread().setContextClassLoader(
                    new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
            JkRunbase runbase = engine.getOrCreateRunbase(new KBeanAction.Container(), true);
            assertEquals(2, runbase.getChildRunbases().size());
            assertEquals("a", runbase.getChildRunbases().get(0).getBaseDir().getFileName().toString());
            assertEquals(dir.resolve("a").toString(), System.getProperty("injected.base"));
            assertEquals(dir.resolve("a").toString(), System.getProperty("injected.runbase"));
        } finally {
            System.clearProperty("injected.base");
            System.clearProperty("injected.runbase");
        }
    }

    @Test
    void childInjectingParent_fromParent_ok() throws IOException {
        Path dir = baseDir.toRealPath();
        write(dir.resolve("jeka.properties"), "_jeka.child-bases=a\n@base=on\n");
        write(dir.resolve("a/jeka.properties"), "");
        write(dir.resolve("a/jeka-src/BuildA.java"),
                "import dev.jeka.core.tool.*;\n" +
                "import dev.jeka.core.tool.builtins.base.BaseKBean;\n" +
                "class BuildA extends KBean {\n" +
                "  @JkInject(\"..\") BaseKBean parentBase;\n" +
                "  public void init() { System.getProperties().put(\"injected.parent\", parentBase); }\n" +
                "}\n");
        try {
            Engine engine = Engine.of(true, dir, JkRepoSet.ofLocal(), JkDependencySet.of());
            engine.resolveKBeans();
            Engines.registerMaster(engine);
            Thread.currentThread().setContextClassLoader(
                    new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
            JkRunbase runbase = engine.getOrCreateRunbase(new KBeanAction.Container(), true);
            assertEquals(1, runbase.getChildRunbases().size());
            assertSame(runbase.find(BaseKBean.class).orElse(null), System.getProperties().get("injected.parent"));
        } finally {
            System.getProperties().remove("injected.parent");
        }
    }

    @Test
    void injectedRunbase_invalidTarget_explicitError() throws IOException {
        Path dir = baseDir.toRealPath();
        Path child = Files.createDirectories(dir.resolve("child"));
        KBeanTestLoader.load(dir, BaseKBean.class);  // set dir as the master runbase
        JkRunbase childRunbase = new JkRunbase(child);

        Exception missing = assertThrows(IllegalStateException.class,
                () -> childRunbase.getInjectedRunbase("../missing"));
        assertTrue(missing.getMessage().contains("mentions a directory that does not exist"), missing.getMessage());

        // The parent is initialized
        assertEquals(dir, childRunbase.getInjectedRunbase("..").getBaseDir());

        // The parent passes the guard
        JkRunbase master = KBeanTestLoader.load(dir, BaseKBean.class).getRunbase();
        Exception fromParent = assertThrows(IllegalStateException.class, () -> master.getInjectedRunbase("missing"));
        assertTrue(fromParent.getMessage().contains("mentions a directory that does not exist"), fromParent.getMessage());

        // The parent is not initialized yet: a KBean of the parent injects the child, which injects the parent back
        JkRunbase.setMaster(new JkRunbase(dir));
        Exception parent = assertThrows(IllegalStateException.class, () -> childRunbase.getInjectedRunbase(".."));
        assertTrue(parent.getMessage().contains("targets the parent runbase"), parent.getMessage());
    }

    @Test
    void adminMode_noChildBase() throws IOException {
        Path dir = baseDir.toRealPath();
        write(dir.resolve("jeka.properties"), "_jeka.child-bases=a\n");
        write(dir.resolve("a/jeka.properties"), "");
        BehaviorSettings.setAdminMode();
        Engine engine = Engine.of(true, dir, JkRepoSet.ofLocal(), JkDependencySet.of());
        engine.resolveKBeans();
        Engines.registerMaster(engine);
        Thread.currentThread().setContextClassLoader(
                new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
        JkRunbase runbase = engine.getOrCreateRunbase(new KBeanAction.Container(), true);
        assertTrue(runbase.getChildRunbases().isEmpty());
    }

    @Test
    void adminMode_ignoresProjectDefaultKBean() throws IOException {
        Path dir = baseDir.toRealPath();
        write(dir.resolve("jeka.properties"), "jeka.kbean.default=script\n");
        BehaviorSettings.setAdminMode();
        Engine engine = Engine.of(true, dir, JkRepoSet.ofLocal(), JkDependencySet.of());
        assertNull(engine.resolveKBeans().defaultKbeanClassName);
        assertNull(new JkRunbase(dir).getProperties().get("jeka.kbean.default"));
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }
}
