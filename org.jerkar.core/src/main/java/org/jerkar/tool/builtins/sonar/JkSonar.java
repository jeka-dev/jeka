package org.jerkar.tool.builtins.sonar;


import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.file.JkPathFile;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Sonar wrapper class for launching sonar analysis in a convenient way. This
 * Sonar wrapper is not specific to Java project so can be used for to analyse
 * any kind of project supported by SonarQube.
 *
 * @author Jerome Angibaud
 */
public final class JkSonar {

    public static final String PROJECT_KEY = "projectKey";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_VERSION = "projectVersion";
    public static final String LANGUAGE = "language";
    public static final String PROFILE = "profile";
    public static final String BRANCH = "branch";
    public static final String SOURCE_ENCODING = "sourceEncoding";
    public static final String VERBOSE = "verbose";
    public static final String WORKING_DIRECTORY = "working.directory";
    public static final String JUNIT_REPORTS_PATH = "junit.reportsPath";
    public static final String SUREFIRE_REPORTS_PATH = "surefire.reportsPath";
    public static final String JACOCO_REPORTS_PATHS = "jacoco.reportPaths";
    public static final String COVERTURA_REPORTS_PATH = "cobertura.reportPath";
    public static final String CLOVER_REPORTS_PATH = "clover.reportPath";
    public static final String DYNAMIC_ANALYSIS = "dynamicAnalysis";
    public static final String PROJECT_BASE_DIR = "projectBaseDir";
    public static final String SOURCES = "sources";
    public static final String BINARIES = "binaries";
    public static final String JAVA_BINARIES = "java.binaries";
    public static final String TEST = "tests";
    public static final String LIBRARIES = "libraries";
    public static final String SKIP_DESIGN = "skipDesign";
    public static final String HOST_URL = "host.url";
    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";
    private static final String RUNNER_JAR_NAME_24 = "sonar-runner-2.4.jar";
    private static final String RUNNER_LOCAL_PATH = JkConstants.OUTPUT_PATH + "/temp/" + RUNNER_JAR_NAME_24;
    private static final String SONAR_PREFIX = "sonar.";
    private final Map<String, String> params;

    private final boolean enabled;

    private JkSonar(Map<String, String> params, boolean enabled) {
        super();
        this.params = Collections.unmodifiableMap(params);
        this.enabled = enabled;
    }

    public static JkSonar of(String projectKey, String projectName, JkVersion version) {
        JkUtilsAssert.notNull(projectName, "Project name can't be null.");
        JkUtilsAssert.notNull(projectKey, "Project key can't be null.");
        JkUtilsAssert.notNull(version, "Project version can't be null.");
        final Map<String, String> map = new HashMap<>();
        map.put(PROJECT_KEY, projectKey);
        map.put(PROJECT_NAME, projectName);
        map.put(PROJECT_VERSION, version.getValue());
        map.put(WORKING_DIRECTORY, ".sonarTempDir");
        map.put(VERBOSE, Boolean.toString(JkLog.Verbosity.VERBOSE == JkLog.verbosity()));
        final Properties properties = System.getProperties();
        for (final Object keyObject : properties.keySet()) {
            final String key = (String) keyObject;
            if (key.startsWith(SONAR_PREFIX)) {
                map.put(key.substring(SONAR_PREFIX.length()), properties.getProperty(key));
            }
        }
        return new JkSonar(map, true);
    }

    private static Path createRunnerJar(Path parent) {
        final Path file = parent.resolve(RUNNER_JAR_NAME_24);
        return JkPathFile.of(file).copyFrom(JkSonar.class.getResource(RUNNER_JAR_NAME_24)).get();
    }

    public void run() {
        if (!enabled) {
            JkLog.info("Sonar analysis skipped.");
        }
        JkLog.execute("Launching Sonar analysis", () -> {
            if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
                javaProcess().runClassSync("org.sonar.runner.Main", "-e", "-X");
            } else {
                javaProcess().runClassSync("org.sonar.runner.Main", "-e");
            }
        });
    }

    public JkSonar enabled(boolean enabled) {
        return new JkSonar(this.params, enabled);
    }

    private JkJavaProcess javaProcess() {
        final Path sonarRunnerJar = JkUtilsObject.firstNonNull(
                JkClassLoader.ofCurrent().getFullClasspath().getEntryContainingClass("org.sonar.runner.Main"),
                jarRunner());

        return JkJavaProcess.of().withClasspath(sonarRunnerJar)
                .andOptions(toProperties());
    }

    private List<String> toProperties() {
        final List<String> result = new LinkedList<>();
        for (final Map.Entry<String, String> entry : this.params.entrySet()) {
            result.add("-Dsonar." + entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    public JkSonar withProperty(String key, String value) {
        return new JkSonar(andParams(key, value), enabled);
    }

    public JkSonar withProperties(Map<String, String> props) {
        final Map<String, String> newProps = new HashMap<>(this.params);
        newProps.putAll(props);
        return new JkSonar(newProps, enabled);
    }

    public JkSonar withProjectBaseDir(Path baseDir) {
        return withProperty(PROJECT_BASE_DIR, baseDir.toAbsolutePath().toString());
    }

    public JkSonar withSourcesPath(Iterable<Path> files) {
        return withProperty(SOURCES, toPaths(JkUtilsPath.disambiguate(files)));
    }


    public JkSonar withTestPath(Iterable<Path> files) {
        return withProperty(TEST, toPaths(files));
    }

    public JkSonar withBinaries(Iterable<Path> files) {
        String path = toPaths(JkUtilsPath.disambiguate(files));
        return withProperty(BINARIES, path).withProperty(JAVA_BINARIES, path);
    }

    public JkSonar withBinaries(Path... files) {
        return withBinaries(Arrays.asList(files));
    }

    public JkSonar withLibraries(Iterable<Path> files) {
        return withProperty(LIBRARIES, toPaths(JkUtilsPath.disambiguate(files)));
    }

    public JkSonar withSkipDesign(boolean skip) {
        return withProperty(SKIP_DESIGN, Boolean.toString(skip));
    }

    public JkSonar withHostUrl(String url) {
        return withProperty(HOST_URL, url);
    }

    public JkSonar withJdbcUrl(String url) {
        return withProperty(JDBC_URL, url);
    }

    public JkSonar withJdbcUserName(String userName) {
        return withProperty(JDBC_USERNAME, userName);
    }

    public JkSonar withJdbcPassword(String pwd) {
        return withProperty(JDBC_PASSWORD, pwd);
    }

    private String toPaths(Iterable<Path> files) {
        final Iterator<Path> it = files.iterator();
        final StringBuilder result = new StringBuilder();
        final Path projectDir = projectDir();
        while (it.hasNext()) {
            final Path file = it.next();
            String path;
            if (file.startsWith(projectDir)) {
                path = projectDir.relativize(file).toString();
            } else {
                path = file.toAbsolutePath().toString();
            }
            result.append(path);
            if (it.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    private Path jarRunner() {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        final Path globalJar = tempDir.resolve("jerkar/" + RUNNER_JAR_NAME_24);
        if (!Files.exists(globalJar)) {
            try {
                return createRunnerJar(tempDir);
            } catch (final Exception e) {
                return createRunnerJar(projectDir().resolve(RUNNER_LOCAL_PATH));
            }
        }
        return globalJar;
    }

    private Map<String, String> andParams(String key, String value) {
        final Map<String, String> newMap = new HashMap<>(this.params);
        newMap.put(key, value);
        return newMap;
    }

    private Path projectDir() {
        return Paths.get(this.params.get(PROJECT_BASE_DIR));
    }

}
