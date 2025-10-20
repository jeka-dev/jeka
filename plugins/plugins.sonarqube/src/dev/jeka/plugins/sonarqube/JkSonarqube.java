/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.http.JkHttpResponse;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkConsoleSpinner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkException;

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
public final class JkSonarqube {

    public static final String DEFAULT_SCANNER__VERSION = "5.0.1.3006";

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
    public static final String TEST_INCLUSIONS = "test.inclusions";
    public static final String LIBRARIES = "libraries";
    public static final String EXCLUSIONS = "exclusions";
    public static final String JAVA_LIBRARIES = "java.libraries";
    public static final String JAVA_TEST_LIBRARIES = "java.test.libraries";
    public static final String JAVA_TEST_BINARIES = "java.test.binaries";
    public static final String SKIP_DESIGN = "skipDesign";
    public static final String HOST_URL = "host.url";
    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";
    private static final String SONAR_PREFIX = "sonar.";

    private final Map<String, String> params = new HashMap<>();

    private JkRepoSet repos;

    private String scannerVersion;

    private boolean logOutput = true;

    private boolean pingServer = true;

    private Path projectBaseDir = Paths.get("");

    private JkSonarqube(JkRepoSet repos,
                        @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:") String scannerVersion) {
        this.repos = repos;
        this.scannerVersion = scannerVersion;
        params.put(WORKING_DIRECTORY, workDir(Paths.get("")).toString());
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
     * Returns a copy of the current {@link JkSonarqube} object with the same field values.
     */
    public JkSonarqube copyWithProperties() {
        JkSonarqube copy = copyWithoutProperties();
        copy.params.putAll(this.params);
        return copy;
    }

    /**
     * Returns the value of the property with the specified key.
     */
    public String getProperty(String key) {
        return params.get(key);
    }

    /**
     * Returns a copy of the current {@link JkSonarqube} object with the same field values, except for the properties
     * "logOutput" and "pingServer" which are set to the same as the original object.
     * This method is useful for creating immutable copies of the object.
     */
    public JkSonarqube copyWithoutProperties() {
        JkSonarqube copy = new JkSonarqube(this.repos, this.scannerVersion);
        copy.logOutput = this.logOutput;
        copy.pingServer = this.pingServer;
        return copy;
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
        String hostUrl = getHostUrl();

        JkLog.startTask("run-sonar-analysis");
        if (pingServer) {
            if (!JkUtilsNet.isStatusOk(hostUrl, JkLog.isVerbose())) {
                throw new JkException("The Sonarqube url %s is not available.%nCheck server " +
                        "or disable this ping check with `-D@sonarqube.pingServer=false`", hostUrl);
            }
        }
        JkConsoleSpinner.of("Running Sonarqube").run(this::runOrFail);
        JkLog.info("SonarQube analysis available at %s/dashboard?id=%s",
                this.getHostUrl(), this.getProperty(JkSonarqube.PROJECT_KEY) );
        JkLog.endTask();
    }

    public QualityGateResponse checkQualityGate() {
        JkLog.startTask("sonar-check-quality-gates");
        Path reportTask = JkSonarqube.workDir(projectBaseDir).resolve("report-task.txt");
        JkUtilsAssert.state(Files.exists(reportTask), "No sonarqube report file %s found. " +
                "Run analysis prior checking quality gates.", reportTask);
        String taskId = JkProperties.ofFile(reportTask).get("ceTaskId");

        // Query for analysisId
        String host = getHostUrl();
        if (!host.endsWith("/")) {
            host =  host + "/";
        }
        Map<String, String> headers = new HashMap<>();
        String sonarToken = getProperty(JkSonarqube.TOKEN);
        if (JkUtilsString.isBlank(sonarToken)) {
            sonarToken = System.getenv("SONAR_TOKEN");
        }
        headers.put("Authorization", "Bearer " + sonarToken);
        String taskUrl = host + "api/ce/task?id=" + taskId;
        JkLog.debug("Extracted taskId=%s from sonarqube report.", taskId);
        boolean pending = true;
        JkLog.debug("Querying for analysisId %s.", taskUrl);
        JkHttpResponse response = null;
        while (pending) {
            response = JkHttpRequest.of(taskUrl).addHeaders(headers).execute();
            response.assertNoError();
            String status = extractJsonValue(response.getBody(), "status");
            pending = "PENDING".equals(status) || "IN_PROGRESS".equals(status);
            if (pending) {
                JkLog.info("Waiting for the analysis to be ready for quality gates...");
                JkUtilsSystem.sleep(2000);
            }
        }
        String analysisId = extractJsonValue(response.getBody(), "analysisId");
        JkUtilsAssert.state(!JkUtilsString.isBlank(analysisId), "Field analysisId not found in %s.", response.getBody());
        JkLog.verbose("Extract analysisId=%s from querying sonarqube server.", analysisId);

        // Query for quality gate
        String gatUrl = host + "api/qualitygates/project_status?analysisId=" + analysisId;
        JkHttpResponse gateResponse = JkHttpRequest.of(gatUrl).addHeaders(headers).execute();
        gateResponse.assertNoError();
        String status = extractJsonValue(gateResponse.getBody(), "status");
        boolean result = !status.equals("ERROR");
        JkLog.info("Result: %s", result? "✅ Ok" : "❌ Fail");
        JkLog.endTask();
        return new QualityGateResponse(result, gateResponse.getBody());
    }

    private void runOrFail() {
        Path jar = getToolJar();
        JkProcResult procResult = javaProcess(jar)
                .addParamsIf(JkLog.verbosity() == JkLog.Verbosity.DEBUG, "-X")
                .exec();
        if (!procResult.hasSucceed()) {
            throw new JkException("SonarScanner command failed. Use --debug to get more details.");
        }
    }

    String getHostUrl() {
        String result = Optional.ofNullable(params.get(HOST_URL)).orElse("http://localhost:9000").trim();
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
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
    public JkSonarqube setPathProperty(String key, Iterable<Path> value) {
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
        this.projectBaseDir = project.getBaseDir();
        final JkCompileLayout prodLayout = project.compilation.layout;
        final JkCompileLayout testLayout = project.test.compilation.layout;
        final Path baseDir = project.getBaseDir();
        JkPathSequence libs = JkPathSequence.of();
        if (provideProdLibs) {
            JkDependencySet deps = project.compilation.dependencies.get()
                    .merge(project.pack.runtimeDependencies.get()).getResult();
            libs = project.dependencyResolver.resolve("production libs", deps).getFiles();
        }
        final Path testReportDir = project.test.getReportDir().toAbsolutePath();
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
                .setPathProperty(SOURCES, prodLayout.resolveSources().getRootDirsOrZipFiles())
                .setPathProperty(TEST, testLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(WORKING_DIRECTORY,
                        project.getBaseDir().toAbsolutePath().resolve(JkConstants.JEKA_WORK_PATH).resolve(".sonarscanner").toString())
                .setProperty(JUNIT_REPORTS_PATH,
                        testReportDir.resolve("junit").toString())
                .setProperty(SUREFIRE_REPORTS_PATH,
                        testReportDir.resolve("junit").toString())
                .setProperty(SOURCE_ENCODING, project.getSourceEncoding())
                .setProperty(JACOCO_XML_REPORTS_PATHS,
                        project.getOutputDir().toAbsolutePath().resolve("jacoco/jacoco.xml").toString())
                .setPathProperty(JAVA_LIBRARIES, libs);

        if (Files.exists(testLayout.getClassDirPath())) {
            this.setPathProperty(JAVA_TEST_BINARIES, testLayout.getClassDirPath());
        }
        if (provideTestLibs) {
            JkDependencySet deps = project.test.compilation.dependencies.get();
            JkPathSequence testLibs = project.dependencyResolver.resolve("test libs (Sonarqube)", deps).getFiles();
            this.setPathProperty(JAVA_TEST_LIBRARIES, testLibs);
        }
        return this;
    }

    /**
     * @see #configureFor(JkProject, boolean, boolean)
     */
    public JkSonarqube configureFor(JkProject project) {
        return configureFor(project, true, false);
    }

    static Path workDir(Path baseDir) {
        return baseDir.resolve(JkConstants.OUTPUT_PATH + "/sonarqube-scanner");
    }

    static String extractJsonValue(String json, String propertyName) {
        String token = "\"" + propertyName + "\"";
        String after = JkUtilsString.substringAfterFirst(json, token);
        String afterFirstQuote = JkUtilsString.substringAfterFirst(after, "\"");
        return JkUtilsString.substringBeforeFirst(afterFirstQuote, "\"");
    }

    private JkJavaProcess javaProcess(Path jar) {
        return JkJavaProcess.ofJava("org.sonarsource.scanner.cli.Main")
                .setClasspath(jar)
                .setFailOnError(JkLog.isVerbose())
                .addParams(toProperties())
                .setLogCommand(JkLog.isVerbose())
                .setLogWithJekaDecorator(JkLog.isVerbose() || !JkLog.isAnimationAccepted());
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
        JkCoordinate coordinate = JkCoordinate.of("org.sonarsource.scanner.cli", "sonar-scanner-cli",
                this.scannerVersion);
        JkCoordinateDependency coordinateDependency = JkCoordinateDependency
                .of(coordinate)
                .withTransitivity(JkTransitivity.NONE);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        dependencyResolver
                .getDefaultParams()
                    .setFailOnDependencyResolutionError(false);
        JkResolveResult resolveResult = dependencyResolver.resolve("fetching sonar-scanner cli",
                JkDependencySet.of().and(coordinateDependency));
        if (resolveResult.getErrorReport().hasErrors()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot find dependency " + coordinate + "\n");
            List<String> versions = repos.findVersionsOf(coordinate.getModuleId().toColonNotation());
            sb.append("Known versions are : \n");
            versions.forEach(name -> sb.append(name + "\n"));
            throw new IllegalStateException(sb.toString());
        }
        JkVersion effectiveVersion = resolveResult.getVersionOf(coordinate.getModuleId());  // Get effective version if specified one is '+'
        JkLog.verbose("Use sonar scanner %s", effectiveVersion);
        return resolveResult.getFiles().getEntries().get(0);
    }


    public static class QualityGateResponse {

        public final boolean success;

        public final String details;

        public QualityGateResponse(boolean success, String details) {
            this.success = success;
            this.details = details;
        }
    }

}
