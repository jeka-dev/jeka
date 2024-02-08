package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkException;

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

    public static final String DEFAULT_SCANNER__VERSION = "4.6.2.2472";

    public static final String PROJECT_KEY = "projectKey";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_VERSION = "projectVersion";
    public static final String LANGUAGE = "language";
    public static final String PROFILE = "profile";
    public static final String BRANCH = "branch";
    public static final String TOKEN = "token";
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

    private JkRepoSet repos;

    private String scannerVersion;

    private boolean logOutput = true;

    private boolean pingServer = true;

    private JkSonarqube(JkRepoSet repos,
                        @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:") String scannerVersion) {
        this.repos = repos;
        this.scannerVersion = scannerVersion;
        params.put(WORKING_DIRECTORY, workDir(Paths.get("")));
    }

    /**
     * Creates a {@link JkSonarqube} object using the embedded scanner.
     */
    public static JkSonarqube ofEmbedded() {
        return new JkSonarqube(null, null);
    }

    /**
     * Creates a {@link JkSonarqube} object configured for the supplied scanner version, fetched from the specified repos.
     * @param scannerVersion The scanner version to use. If <code>null</code>, the embedded scanner version will
     *                       be used.
     */
    public static JkSonarqube ofVersion(JkRepoSet repos,
                                        @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:") String scannerVersion) {
        return new JkSonarqube(repos, scannerVersion);
    }

    /**
     * @see #ofVersion(JkRepoSet, String)
     */
    public static JkSonarqube ofVersion(JkDependencyResolver dependencyResolver,
                                        @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:") String scannerVersion) {
        return new JkSonarqube(dependencyResolver.getRepos(), scannerVersion);
    }

    /**
     * Creates a {@link JkSonarqube} object configured for the supplied scanner version, fetched from Maven central.
     * @param scannerVersion The scanner version to use. If <code>null</code>, the embedded scanner version will
     *                       be used.
     */
    public static JkSonarqube ofVersion(@JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:") String scannerVersion) {
        return ofVersion(JkRepo.ofMavenCentral().toSet(), scannerVersion);
    }

    /**
     * Sets the version of the SonarQube scanner and the repositories from which to download it.
     *
     * @param downloadRepos The repositories from which to download the SonarQube scanner.
     * @param scannerVersion The version of the SonarQube scanner. It should be in the format "org.sonarsource.scanner.cli:sonar-scanner-cli:VERSION".
     */
    public JkSonarqube setVersion(JkRepoSet downloadRepos, @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:") String scannerVersion) {
        this.repos = downloadRepos;
        this.scannerVersion = scannerVersion;
        return this;
    }

    /**
     * Sets the project ID for SonarQube analysis.
     */
    public JkSonarqube setProjectId(String projectKey, String projectName, String version) {
        final Map<String, String> map = new HashMap<>();
        map.put(PROJECT_KEY, projectKey);
        map.put(PROJECT_NAME, projectName);
        map.put(PROJECT_VERSION, version);
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

    /**
     * Determines whether SonarQube scanner logs should be displayed.
     *
     * @param logOutput 'true' if the logs should be displayed, 'false' otherwise.
     * @return an instance of JkSonarqube with the updated logOutput setting.
     */
    public JkSonarqube setLogOutput(boolean logOutput) {
        this.logOutput = logOutput;
        return this;
    }

    /**
     * Sets the flag whether to ping the server during SonarQube analysis.
     */
    public JkSonarqube setPingServer(boolean pingServer) {
        this.pingServer = pingServer;
        return this;
    }

    /**
     * Executes SonarQube analysis.
     */
    public void run() {
        String hostUrl = Optional.ofNullable(params.get(HOST_URL)).orElse("https://localhost:9000");
        if (pingServer) {
            if (!JkUtilsNet.isStatusOk(hostUrl)) {
                throw new JkException("The Sonarqube url %s is not available.%nCheck server " +
                        "or disable this ping check (sonarqube#pingServer=false)", hostUrl);
            }
        }
        JkLog.startTask("Launch Sonar analysis on server " + hostUrl);

        Path jar = getToolJar();
        JkProcResult procResult = javaProcess(jar)
                .addParamsIf(JkLog.isVerbose(), "-X")
                .exec();
        if (!procResult.hasSucceed()) {
            throw new JkException("SonarScanner command failed. Use--verbose to get more details.");
        }
        JkLog.endTask();
    }

    /**
     * Sets a sonarqube property (aka analysis parameter) as listed <a href="https://docs.sonarsource.com/sonarqube/9.9/analyzing-source-code/analysis-parameters/">here</a>. <p/>
     *
     * Sonarqube properties all start with prefix 'sonar.' but you can omit it, as it will be
     * prepended at launch time, if missing.
     */
    public JkSonarqube setProperty(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * Sets a sonarqube property (aka analysis parameter) with a list of paths.
     *
     * @param key   The key of the property.
     * @param value The iterable collection of paths.
     * @return An instance of JkSonarqube with the updated property.
     * @see <a href="https://docs.sonarsource.com/sonarqube/9.9/analyzing-source-code/analysis-parameters/">SonarQube Analysis Parameters</a>
     */
    public JkSonarqube setProperty(String key, Iterable<Path> value) {
        return setProperty(key, toPaths(value));
    }

    /**
     * Adds SonarQube analysis properties.
     */
    public JkSonarqube setProperties(Map<String, String> props) {
        this.params.putAll(props);
        return this;
    }

    /**
     * Sets all properties stating with 'sonar." prefix from the specified {@link JkProperties object}.
     * @see #setProperty(String, String)
     */
    public JkSonarqube setProperties(JkProperties properties) {
        this.setProperties(properties.getAllStartingWith("sonar.", false));
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

    /**
     * Configures Sonarqube for the supplied {@link JkProject}.
     * @param provideProdLibs If true, the list of production dependency files will be provided to sonarqube.
     * @param provideTestLibs If true, the list of test dependency files will be provided to sonarqube.
     */
    public JkSonarqube configureFor(JkProject project, boolean provideProdLibs, boolean provideTestLibs) {
        final JkCompileLayout prodLayout = project.compilation.layout;
        final JkCompileLayout testLayout = project.testing.compilation.layout;
        final Path baseDir = project.getBaseDir();
        JkPathSequence libs = JkPathSequence.of();
        if (provideProdLibs) {
            JkDependencySet deps = project.compilation.getDependencies()
                    .merge(project.packaging.getRuntimeDependencies()).getResult();
            libs = project.dependencyResolver.resolve(deps).getFiles();
        }
        final Path testReportDir = project.testing.getReportDir();
        JkModuleId jkModuleId = project.getModuleId();
        if (jkModuleId == null) {
            String baseDirName = baseDir.getFileName().toString();
            if (JkUtilsString.isBlank(baseDirName)) {
                baseDirName = baseDir.toAbsolutePath().getFileName().toString();
            }
            jkModuleId = JkModuleId.of(baseDirName, baseDirName);
        }
        final String version = project.getVersion().getValue();
        final String fullName = jkModuleId.getDotNotation();
        final String name = jkModuleId.getName();
        this
                .setProjectId(fullName, name, version)
                .setProjectBaseDir(baseDir)
                .setBinaries(project.compilation.layout.resolveClassDir())
                .setProperty(VERBOSE, Boolean.toString(JkLog.isVerbose()))
                .setProperty(SOURCES, prodLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(TEST, testLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(WORKING_DIRECTORY, workDir(baseDir))
                .setProperty(JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .setProperty(SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .setProperty(SOURCE_ENCODING, project.getSourceEncoding())
                .setProperty(JACOCO_XML_REPORTS_PATHS,
                        baseDir.relativize(project.getOutputDir().resolve("jacoco/jacoco.xml")).toString())
                .setProperty(JAVA_LIBRARIES, libs)
                .setProperty(JAVA_TEST_BINARIES, testLayout.getClassDirPath());
        if (provideTestLibs) {
            JkDependencySet deps = project.testing.compilation.getDependencies();
            JkPathSequence testLibs = project.dependencyResolver.resolve(deps).getFiles();
            this.setProperty(JAVA_TEST_LIBRARIES, testLibs);
        }
        return this;
    }

    /**
     * @see #configureFor(JkProject, boolean, boolean)
     */
    public JkSonarqube configureFor(JkProject project) {
        return configureFor(project, true, false);
    }

    private static String workDir(Path baseDir) {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH + "/.sonarscannerworks").toString();
    }

    private JkJavaProcess javaProcess(Path jar) {
        return JkJavaProcess.ofJava("org.sonarsource.scanner.cli.Main")
                .setClasspath(jar)
                .setFailOnError(JkLog.isVerbose())
                .addParams(toProperties())
                .setLogCommand(JkLog.isVerbose())
                .setInheritIO(logOutput)
                .setLogWithJekaDecorator(false);
    }

    private List<String> toProperties() {
        final List<String> result = new LinkedList<>();
        for (final Map.Entry<String, String> entry : this.params.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(SONAR_PREFIX)) {
                key = SONAR_PREFIX + key;
            }
            result.add("-D" + key + "=" + entry.getValue());
        }
        return result;
    }

    private String toPaths(Iterable<Path> files) {
        final Iterator<Path> it = JkUtilsPath.disambiguate(files).iterator();
        final StringBuilder result = new StringBuilder();
        final Path projectDir = projectDir();
        while (it.hasNext()) {
            final Path file = it.next();
            String path;
            if (file.startsWith(projectDir)) {
                path = projectDir.relativize(file).normalize().toString();
            } else {
                path = file.toAbsolutePath().normalize().toString();
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
        if (this.scannerVersion == null) {
            URL embeddedUrl = JkSonarqube.class.getResource(SCANNER_JAR_NAME_46);
            JkLog.info("Use embedded sonar scanner : " + SCANNER_JAR_NAME_46);
            return JkUtilsIO.copyUrlContentToCacheFile(embeddedUrl, null, JkInternalEmbeddedClassloader.URL_CACHE_DIR);
        }
        JkCoordinate coordinate = JkCoordinate.of("org.sonarsource.scanner.cli", "sonar-scanner-cli",
                this.scannerVersion);
        JkCoordinateDependency coordinateDependency = JkCoordinateDependency
                .of(coordinate)
                .withTransitivity(JkTransitivity.NONE);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        dependencyResolver
                .getDefaultParams()
                    .setFailOnDependencyResolutionError(false);
        JkResolveResult resolveResult = dependencyResolver.resolve(JkDependencySet.of().and(coordinateDependency));
        if (resolveResult.getErrorReport().hasErrors()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot find dependency " + coordinate + "\n");
            List<String> versions = dependencyResolver.searchVersions(coordinate.getModuleId());
            sb.append("Known versions are : \n");
            versions.forEach(name -> sb.append(name + "\n"));
            throw new IllegalStateException(sb.toString());
        }
        JkVersion effectiveVersion = resolveResult.getVersionOf(coordinate.getModuleId());  // Get effective version if specified one is '+'
        JkLog.info("Run sonar scanner " + effectiveVersion);
        return resolveResult.getFiles().getEntries().get(0);
    }



}
