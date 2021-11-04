package dev.jeka.plugins.sonarqube;


import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.net.URL;
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
public final class JkSonarqube {

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
    public static final String JACOCO_LEGACY_REPORTS_PATHS = "jacoco.reportPaths";
    public static final String JACOCO_XML_REPORTS_PATHS = "coverage.jacoco.xmlReportPaths";
    public static final String COVERTURA_REPORTS_PATH = "cobertura.reportPath";
    public static final String CLOVER_REPORTS_PATH = "clover.reportPath";
    public static final String DYNAMIC_ANALYSIS = "dynamicAnalysis";
    public static final String PROJECT_BASE_DIR = "projectBaseDir";
    public static final String SOURCES = "sources";
    public static final String BINARIES = "binaries";
    public static final String JAVA_BINARIES = "java.binaries";
    public static final String TEST = "tests";
    public static final String LIBRARIES = "libraries";
    public static final String JAVA_LIBRARIES = "java.libraries";
    public static final String JAVA_TEST_LIBRARIES = "java.test.libraries";
    public static final String JAVA_TEST_BINARIES = "java.test.binaries";
    public static final String SKIP_DESIGN = "skipDesign";
    public static final String HOST_URL = "host.url";
    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";
    private static final String SCANNER_JAR_NAME_46 = "sonar-scanner-cli-4.6.2.2472.jar";
    private static final String SONAR_PREFIX = "sonar.";

    private final Map<String, String> params = new HashMap<>();

    private final JkRepoSet repos;

    private final String sonnarScannerVersion;

    private boolean logOutput;

    private JkSonarqube(JkRepoSet repos, String sonarScannerVersion) {
        this.repos = repos;
        this.sonnarScannerVersion = sonarScannerVersion;
    }

    public static JkSonarqube ofEmbedded() {
        return new JkSonarqube(null, null);
    }

    public static JkSonarqube ofVersion(JkRepoSet repos, String version) {
        return new JkSonarqube(repos, version);
    }

    public static JkSonarqube ofVersion(String version) {
        return ofVersion(JkRepo.ofMavenCentral().toSet(), version);
    }


    public JkSonarqube setProjectId(String projectKey, String projectName, String version) {
        final Map<String, String> map = new HashMap<>();
        map.put(PROJECT_KEY, projectKey);
        map.put(PROJECT_NAME, projectName);
        map.put(PROJECT_VERSION, version);
        map.put(WORKING_DIRECTORY, ".sonarTempDir");
        map.put(VERBOSE, Boolean.toString(JkLog.Verbosity.VERBOSE == JkLog.verbosity()));
        final Properties properties = System.getProperties();
        for (final Object keyObject : properties.keySet()) {
            final String key = (String) keyObject;
            if (key.startsWith(SONAR_PREFIX)) {
                map.put(key.substring(SONAR_PREFIX.length()), properties.getProperty(key));
            }
        }
        this.setProperties(map);
        return this;
    }

    public JkSonarqube setLogOutput(boolean logOutput) {
        this.logOutput = logOutput;
        return this;
    }

    public void run() {
        JkLog.startTask("Launch Sonar analysis");
        Path jar = getToolJar();
        String[] args = JkLog.isVerbose() ? new String[] {"-e", "-X"} : new String[] {"-e"};
        javaProcess(jar, "org.sonarsource.scanner.cli.Main").exec(args);
        JkLog.endTask();
    }

    private JkJavaProcess javaProcess(Path jar, String mainClassName) {
        return JkJavaProcess.ofJava(mainClassName)
                .setClasspath(jar)
                .setFailOnError(true)
                .addParams(toProperties())
                .setLogCommand(JkLog.isVerbose())
                .setLogOutput(JkLog.isVerbose() || logOutput);
    }

    private List<String> toProperties() {
        final List<String> result = new LinkedList<>();
        for (final Map.Entry<String, String> entry : this.params.entrySet()) {
            result.add("-Dsonar." + entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    public JkSonarqube setProperty(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    public JkSonarqube setProperty(String key, Iterable<Path> value) {
        return setProperty(key, toPaths(value));
    }

    public JkSonarqube setProperties(Map<String, String> props) {
        this.params.putAll(props);
        return this;
    }

    public JkSonarqube setProjectBaseDir(Path baseDir) {
        return setProperty(PROJECT_BASE_DIR, baseDir.toAbsolutePath().toString());
    }

    public JkSonarqube setBinaries(Iterable<Path> files) {
        String path = toPaths(JkUtilsPath.disambiguate(files));
        return setProperty(BINARIES, path).setProperty(JAVA_BINARIES, path);
    }

    public JkSonarqube setBinaries(Path... files) {
        return setBinaries(Arrays.asList(files));
    }

    public JkSonarqube setSkipDesign(boolean skip) {
        return setProperty(SKIP_DESIGN, Boolean.toString(skip));
    }

    public JkSonarqube setHostUrl(String url) {
        return setProperty(HOST_URL, url);
    }

    public JkSonarqube setJdbcUrl(String url) {
        return setProperty(JDBC_URL, url);
    }

    public JkSonarqube setJdbcUserName(String userName) {
        return setProperty(JDBC_USERNAME, userName);
    }

    public JkSonarqube setJdbcPassword(String pwd) {
        return setProperty(JDBC_PASSWORD, pwd);
    }

    private String toPaths(Iterable<Path> files) {
        final Iterator<Path> it = JkUtilsPath.disambiguate(files).iterator();
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

    private Path projectDir() {
        return Paths.get(this.params.get(PROJECT_BASE_DIR));
    }

    private Path getToolJar() {
        JkJavaVersion javaVersion = JkJavaVersion.of(System.getProperty("java.version"));
        JkUtilsAssert.state(javaVersion.compareTo(JkJavaVersion.V11) >= 0,
                "Sonarqube has to run on JRE >= 11. You are running on version " + javaVersion);
        if (this.sonnarScannerVersion == null) {
            URL embeddedUrl = JkSonarqube.class.getResource(SCANNER_JAR_NAME_46);
            JkLog.info("Use embedded sonar scanner : " + SCANNER_JAR_NAME_46);
            return JkUtilsIO.copyUrlContentToCacheFile(embeddedUrl, null, JkInternalClassloader.URL_CACHE_DIR);
        }
        JkModuleDependency moduleDep = JkModuleDependency
                .of("org.sonarsource.scanner.cli", "sonar-scanner-cli", this.sonnarScannerVersion)
                .withTransitivity(JkTransitivity.NONE);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of()
                .addRepos(repos)
                .getParams()
                    .setFailOnDependencyResolutionError(false)
                .__;
        JkResolveResult resolveResult = dependencyResolver.resolve(JkDependencySet.of().and(moduleDep));
        if (resolveResult.getErrorReport().hasErrors()) {
            StringBuilder sb = new StringBuilder();
            String coordinates =  moduleDep.getModuleId().withVersion(this.sonnarScannerVersion).toString();
            sb.append("Cannot find dependency " + coordinates + "\n");
            List<String> versions = dependencyResolver.searchVersions(moduleDep.getModuleId());
            sb.append("Known versions are : \n");
            versions.forEach(name -> sb.append(name + "\n"));
            throw new IllegalStateException(sb.toString());
        }
        JkVersion effectiveVersion = resolveResult.getVersionOf(moduleDep.getModuleId());  // Get effective version if specified one is '+'
        JkLog.info("Run sonar scanner " + effectiveVersion);
        return resolveResult.getFiles().getEntries().get(0);
    }

}
