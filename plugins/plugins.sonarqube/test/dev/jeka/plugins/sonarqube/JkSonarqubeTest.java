package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JkSonarqubeTest {

    private static JkSonarqube sonarqube() {
        return JkSonarqube.ofVersion(JkRepoSet.of(), "1.0");
    }

    @Test
    void setters_setTheirProperty() {
        JkSonarqube sonarqube = sonarqube()
                .setHostUrl("http://host")
                .setJdbcUrl("jdbc:x")
                .setJdbcUserName("user")
                .setJdbcPassword("pwd")
                .setSkipDesign(true)
                .setProperty("my.key", "my-value");

        assertEquals("http://host", sonarqube.getProperty(JkSonarqube.HOST_URL));
        assertEquals("jdbc:x", sonarqube.getProperty(JkSonarqube.JDBC_URL));
        assertEquals("user", sonarqube.getProperty(JkSonarqube.JDBC_USERNAME));
        assertEquals("pwd", sonarqube.getProperty(JkSonarqube.JDBC_PASSWORD));
        assertEquals("true", sonarqube.getProperty(JkSonarqube.SKIP_DESIGN));
        assertEquals("my-value", sonarqube.getProperty("my.key"));
        assertEquals(JkSonarqube.workDir(Paths.get("")).toString(),
                sonarqube.getProperty(JkSonarqube.WORKING_DIRECTORY));
    }

    @Test
    void copies_withOrWithoutProperties() {
        JkSonarqube sonarqube = sonarqube().setHostUrl("http://host");

        assertEquals("http://host", sonarqube.copyWithProperties().getProperty(JkSonarqube.HOST_URL));
        assertNull(sonarqube.copyWithoutProperties().getProperty(JkSonarqube.HOST_URL));
    }

    @Test
    void setProperties_fromJkProperties_keepsSonarOnesWithoutPrefix() {
        JkProperties properties = JkProperties.ofMap(Map.of("sonar.host.url", "http://host", "other.key", "x"));
        JkSonarqube sonarqube = sonarqube().setProperties(properties);

        assertEquals("http://host", sonarqube.getProperty(JkSonarqube.HOST_URL));
        assertNull(sonarqube.getProperty("other.key"));
    }

    @Test
    void setProjectId_includesSonarSystemProperties() {
        System.setProperty("sonar.jk-test.key", "from-sys");
        try {
            JkSonarqube sonarqube = sonarqube().setProjectId("group.name", "name", "1.2");
            assertEquals("group.name", sonarqube.getProperty(JkSonarqube.PROJECT_KEY));
            assertEquals("name", sonarqube.getProperty(JkSonarqube.PROJECT_NAME));
            assertEquals("1.2", sonarqube.getProperty(JkSonarqube.PROJECT_VERSION));
            assertEquals("from-sys", sonarqube.getProperty("jk-test.key"));
        } finally {
            System.clearProperty("sonar.jk-test.key");
        }
    }

    @Test
    void pathProperties_relativeToProjectBaseDir(@TempDir Path dir) {
        Path baseDir = dir.toAbsolutePath();
        JkSonarqube sonarqube = sonarqube()
                .setProjectBaseDir(baseDir)
                .setBinaries(baseDir.resolve("classes"))
                .setPathProperty("other.paths", Collections.singletonList(baseDir.resolve("a/b")));

        assertEquals("classes", sonarqube.getProperty(JkSonarqube.BINARIES));
        assertEquals("classes", sonarqube.getProperty(JkSonarqube.JAVA_BINARIES));
        assertEquals(Paths.get("a/b").toString(), sonarqube.getProperty("other.paths"));
    }

    @Test
    void configureFor_project_setsProjectProperties(@TempDir Path dir) throws Exception {
        Path baseDir = dir.toRealPath();
        Files.createDirectories(baseDir.resolve("src/main/java"));
        JkProject project = JkProject.of().setBaseDir(baseDir);
        project.setModuleId("my.group:my-lib").setVersion("1.0");

        JkSonarqube sonarqube = sonarqube().configureFor(project, false, false);

        assertEquals("my.group.my-lib", sonarqube.getProperty(JkSonarqube.PROJECT_KEY));
        assertEquals("my-lib", sonarqube.getProperty(JkSonarqube.PROJECT_NAME));
        assertEquals("1.0", sonarqube.getProperty(JkSonarqube.PROJECT_VERSION));
        assertEquals(baseDir.toString(), sonarqube.getProperty(JkSonarqube.PROJECT_BASE_DIR));
        assertEquals(Paths.get("src/main/java").toString(), sonarqube.getProperty(JkSonarqube.SOURCES));
        assertEquals(project.getSourceEncoding(), sonarqube.getProperty(JkSonarqube.SOURCE_ENCODING));
        assertTrue(sonarqube.getProperty(JkSonarqube.WORKING_DIRECTORY).endsWith(".sonarscanner"),
                sonarqube.getProperty(JkSonarqube.WORKING_DIRECTORY));
        assertEquals("", sonarqube.getProperty(JkSonarqube.JAVA_LIBRARIES));
        assertNull(sonarqube.getProperty(JkSonarqube.JAVA_TEST_LIBRARIES));
    }

    @Test
    void extractJsonValue_ok() {
        String json = "{\"projectStatus\":{\"status\":\"ERROR\",\"other\":\"x\"}}";
        assertEquals("ERROR", JkSonarqube.extractJsonValue(json, "status"));
        assertEquals("x", JkSonarqube.extractJsonValue(json, "other"));
    }

}
