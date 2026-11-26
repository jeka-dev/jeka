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

    private static final String INIT_PROP = "child.only.init";

    private static final String HI_PROP = "child.only.hi";

    @TempDir
    Path baseDir;

    private Path dir;

    private BehaviorSettings initialSettings;

    private ClassLoader initialClassloader;

    private Engine engine;

    @BeforeEach
    void setUp() throws IOException {
        initialSettings = BehaviorSettings.INSTANCE;
        initialClassloader = Thread.currentThread().getContextClassLoader();
        BehaviorSettings.INSTANCE = BehaviorSettings.ofDefault();

        dir = baseDir.toRealPath();
        write(dir.resolve("jeka.properties"), "_jeka.child-bases=mods/a\n");
        write(dir.resolve("mods/a/jeka.properties"), "");

        // A KBean existing only in the child
        write(dir.resolve("mods/a/jeka-src/ChildOnly.java"),
                "import dev.jeka.core.tool.KBean;\n" +
                "public class ChildOnly extends KBean {\n" +
                "  protected void init() {\n" +
                "    String previous = System.getProperty(\"" + INIT_PROP + "\", \"\");\n" +
                "    System.setProperty(\"" + INIT_PROP + "\", previous + getBaseDir().toAbsolutePath() + \";\");\n" +
                "  }\n" +
                "  public void hi() { System.setProperty(\"" + HI_PROP + "\", getBaseDir().toAbsolutePath().toString()); }\n" +
                "}\n");

        engine = Engine.of(true, dir, JkRepoSet.ofLocal(), JkDependencySet.of());
        engine.resolveKBeans();
        Engines.registerMaster(engine);
        Thread.currentThread().setContextClassLoader(
                new AppendableClassloader(engine.getClasspathSetupResult().runClasspath));
    }

    @AfterEach
    void tearDown() {
        BehaviorSettings.INSTANCE = initialSettings;
        Thread.currentThread().setContextClassLoader(initialClassloader);
        System.clearProperty(INIT_PROP);
        System.clearProperty(HI_PROP);
    }

    @Test
    void undeclaredChildBase_fails() {
        setChildBase("mods/b", false);
        JkException e = assertThrows(JkException.class, this::run);
        assertNotFoundMessage(e);
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

    @Test
    void kbeanOnlyInChild_runsInChild() {
        setChildBase("mods/a", false);
        KBeanAction.Container actions = CmdLineParser.parse(new CmdLineArgs("childOnly:", "hi"), cmdLineResolution());

        JkRunbase runbase = engine.getOrCreateRunbase(actions, true);
        runbase.run(actions, true);

        String childDir = dir.resolve("mods/a").toString();
        assertEquals(childDir, System.getProperty(HI_PROP));
        assertEquals(childDir + ";", System.getProperty(INIT_PROP), "Initialized in the parent too");
    }

    @Test
    void kbeanOnlyInChild_withoutChildBase_notFound() {
        assertThrows(CommandLine.ParameterException.class,
                () -> CmdLineParser.parse(new CmdLineArgs("childOnly:", "hi"), cmdLineResolution()));
    }

    @Test
    void cmdLineResolution_currentBase_isTheBaseOne() {
        setChildBase(".", false);
        assertSame(engine.getKbeanResolution(), cmdLineResolution());
    }

    @Test
    void cmdLineResolution_undeclaredChildBase_failsUnlessForce() {
        setChildBase("mods/b", false);
        assertNotFoundMessage(assertThrows(JkException.class, this::cmdLineResolution));

        setChildBase("mods/b", true);
        assertSame(engine.getKbeanResolution(), cmdLineResolution());
    }

    private KBeanResolution cmdLineResolution() {
        return Main.cmdLineKBeanResolution(engine, new JkRunbase(dir).getProperties(), engine.getKbeanResolution());
    }

    private void run() {
        JkRunbase runbase = engine.getOrCreateRunbase(new KBeanAction.Container(), true);
        runbase.run(new KBeanAction.Container(), true);
    }

    private static void assertNotFoundMessage(Exception e) {
        assertTrue(e.getMessage().contains("No child base found at location mods/b"), e.getMessage());
        assertTrue(e.getMessage().contains("[mods/a]"), e.getMessage());
        assertTrue(e.getMessage().contains("--force"), e.getMessage());
    }

    private static void setChildBase(String childBase, boolean force) {
        BehaviorSettings.INSTANCE = new BehaviorSettings(null, false, false, force, false, childBase, false, false);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }

}
