package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.system.JkProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesHandlerTest {

    private static final String REPO_URL = "https://my.repo.example/maven";

    @TempDir
    Path baseDir;

    @BeforeEach
    void writeJekaProperties() throws IOException {
        String content = "jeka.cmd.zzq=project: pack\njeka.repos.download=" + REPO_URL + "\n";
        Files.write(baseDir.resolve(JkConstants.PROPERTIES_FILE), content.getBytes());
    }

    @Test
    void constructProperties_regularKBean_usesJekaProperties() {
        JkProperties props = masterProperties("::zzq");
        assertArrayEquals(new String[] {"project:", "pack"},
                new CmdLineArgs("::zzq").interpolated(props).get());
        assertTrue(JkRepoProperties.of(props).getDownloadRepos().getRepos().stream()
                .anyMatch(repo -> repo.getUrl().toString().startsWith(REPO_URL)));
    }

    @Test
    void constructProperties_adminKBean_ignoresJekaProperties() {
        assertNull(masterProperties("app:", "list").get("jeka.cmd.zzq"));
        assertNull(masterProperties("setup:", "openHomeDir").get("jeka.cmd.zzq"));
    }

    @Test
    void interpolation_fromEnvSystemParentAndLocal_ok() throws IOException {
        Path child = Files.createDirectories(baseDir.resolve("child"));
        write(baseDir, "a=parent\nref=${a}\nplus=p\n_hidden=h\n");
        write(child, "a=child\nown=${a}\nfromParent=${ref}\nuser=${user.name}\npath=${PATH}\n+plus=${a}\n_loc=${a}\nloc2=${loc}\n");
        JkProperties props = PropertiesHandler.constructRunbaseProperties(child);
        assertEquals("child", props.get("own"));
        assertEquals("parent", props.get("ref"));   // the child can't alter parent references
        assertEquals("parent", props.get("fromParent"));
        assertEquals(System.getProperty("user.name"), props.get("user"));
        assertEquals(System.getenv("PATH"), props.get("path"));
        assertEquals("p child", props.get("plus"));
        assertEquals("child", props.get("loc2"));
        assertNull(props.get("hidden"));
    }

    @Test
    void interpolation_undefined_fails() throws IOException {
        write(baseDir, "foo=${undefined.zzz}\n");
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> PropertiesHandler.constructRunbaseProperties(baseDir));
        assertTrue(e.getMessage().contains("'foo' references '${undefined.zzz}'"));
    }

    private static void write(Path dir, String content) throws IOException {
        Files.write(dir.resolve(JkConstants.PROPERTIES_FILE), content.getBytes());
    }

    private JkProperties masterProperties(String... args) {
        return PropertiesHandler.constructProperties(baseDir, new CmdLineArgs(args).containsAdminKBeans());
    }
}
