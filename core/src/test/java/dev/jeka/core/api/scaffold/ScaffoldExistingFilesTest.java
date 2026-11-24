package dev.jeka.core.api.scaffold;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.base.JkBaseScaffold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scaffolding must not alter existing files.
 */
class ScaffoldExistingFilesTest {

    // Longer than any generated file, to detect partial overwriting
    private static final String USER_CONTENT = "# user content\n" + "# line\n".repeat(2000);

    @TempDir
    Path baseDir;

    @TempDir
    Path generatedDir;

    @Test
    void project_allKinds_existingFilesKept() {
        for (JkProjectScaffold.Kind kind : JkProjectScaffold.Kind.values()) {
            Path dir = baseDir.resolve(kind.name());
            assertExistingFilesKept(dir, () -> projectScaffold(dir).setKind(kind).setGenerateLibsFolders(true).run());
        }
    }

    @Test
    void base_allKinds_existingFilesKept() {
        for (JkBaseScaffold.Kind kind : JkBaseScaffold.Kind.values()) {
            Path dir = baseDir.resolve(kind.name());
            BaseKBean.BaseScaffoldOptions options = new BaseKBean.BaseScaffoldOptions();
            options.kind = kind;
            assertExistingFilesKept(dir, () ->
                    JkBaseScaffold.of(dir, options).setJekaVersion(JkScaffold.UNSPECIFIED_JEKA_VERSION).run());
        }
    }

    @Test
    void emptyJekaProps_kept() {
        JkPathFile.of(propsFile()).write("");
        projectScaffold(baseDir).run();
        assertEquals("", read(propsFile()));
    }

    @Test
    void requestedProps_appendedToExistingJekaProps() {
        String existing = "# my comment\nmy.prop=my value\n@project.version=1.0";
        JkPathFile.of(propsFile()).write(existing);
        JkProjectScaffold scaffold = projectScaffold(baseDir);
        scaffold.addRequestedJekaPropValue("").addRequestedJekaPropValue("@project.version=2.0")
                .addRequestedJekaPropValue("requested.prop=bar").addJekaPropValue("generated.prop=foo");
        scaffold.run();

        assertEquals(existing + "\nrequested.prop=bar\n", read(propsFile()));

        // A second run changes nothing
        JkProjectScaffold second = projectScaffold(baseDir);
        second.addRequestedJekaPropValue("requested.prop=bar");
        second.run();
        assertEquals(existing + "\nrequested.prop=bar\n", read(propsFile()));
    }

    @Test
    void requestedProps_partOfNewJekaProps() throws Exception {
        JkProjectScaffold scaffold = projectScaffold(baseDir);
        scaffold.addRequestedJekaPropValue("requested.prop=bar").addJekaPropValue("generated.prop=foo");
        scaffold.run();
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(propsFile())) {
            properties.load(reader);
        }
        assertEquals("bar", properties.getProperty("requested.prop"));
        assertEquals("foo", properties.getProperty("generated.prop"));
    }

    @Test
    void rawJekaProps_existingJekaPropsKept() {
        Path raw = JkPathFile.of(generatedDir.resolve("raw.properties")).write("raw=true\n").get();
        JkPathFile.of(propsFile()).write("mine=true\n");
        JkProjectScaffold scaffold = projectScaffold(baseDir);
        scaffold.setRawJekaPropsPath(raw);
        scaffold.run();
        assertEquals("mine=true\n", read(propsFile()));
    }

    @Test
    void gitIgnore_completedOnce() {
        Path gitIgnore = baseDir.resolve(".gitignore");
        JkPathFile.of(gitIgnore).write("/mine");
        projectScaffold(baseDir).run();
        projectScaffold(baseDir).run();
        assertEquals(List.of("/mine", "/jeka-output", "/.jeka-work"), JkPathFile.of(gitIgnore).readAsLines());
    }

    private void assertExistingFilesKept(Path dir, Runnable scaffold) {

        // First run to know which files are generated
        scaffold.run();
        Map<Path, String> expected = new TreeMap<>();
        for (Path file : files(dir)) {
            if (!file.endsWith(".gitignore")) {
                JkPathFile.of(file).write(USER_CONTENT);
                expected.put(file, USER_CONTENT);
            }
        }
        assertTrue(expected.containsKey(dir.resolve(JkConstants.PROPERTIES_FILE)));

        scaffold.run();
        expected.forEach((file, content) -> assertEquals(content, read(file), file + " has been modified"));
    }

    private static List<Path> files(Path dir) {
        try (Stream<Path> stream = JkUtilsPath.walk(dir)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    private static JkProjectScaffold projectScaffold(Path dir) {
        JkProjectScaffold result = JkProjectScaffold.of(JkProject.of().setBaseDir(dir));
        result.setJekaVersion(JkScaffold.UNSPECIFIED_JEKA_VERSION);
        return result.setKind(JkProjectScaffold.Kind.EMPTY);
    }

    private Path propsFile() {
        return baseDir.resolve(JkConstants.PROPERTIES_FILE);
    }

    private static String read(Path file) {
        return JkPathFile.of(file).readAsString();
    }

}
