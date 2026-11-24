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
    void constructMasterProperties_regularKBean_usesJekaProperties() {
        JkProperties props = masterProperties("::zzq");
        assertArrayEquals(new String[] {"project:", "pack"},
                new CmdLineArgs("::zzq").interpolated(props).get());
        assertTrue(JkRepoProperties.of(props).getDownloadRepos().getRepos().stream()
                .anyMatch(repo -> repo.getUrl().toString().startsWith(REPO_URL)));
    }

    @Test
    void constructMasterProperties_adminKBean_ignoresJekaProperties() {
        assertNull(masterProperties("app:", "list").get("jeka.cmd.zzq"));
        assertNull(masterProperties("setup:", "openHomeDir").get("jeka.cmd.zzq"));
    }

    private JkProperties masterProperties(String... args) {
        return PropertiesHandler.constructMasterProperties(baseDir, new CmdLineArgs(args).containsAdminKBeans());
    }
}
