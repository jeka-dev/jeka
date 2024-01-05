package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides convenient methods to deal with Jacoco agent and report tool.
 *
 * Note : May sometime fall in this issue when running from IDE :
 * https://stackoverflow.com/questions/31720139/jacoco-code-coverage-report-generator-showing-error-classes-in-bundle-code-c
 *
 * See command-line documentation :
 * https://www.jacoco.org/jacoco/trunk/doc/cli.html
 * https://www.jacoco.org/jacoco/trunk/doc/agent.html
 *
 */
public final class JkJacoco {

    /**
     * Relative location to the output folder of the generated jacoco report file
     */
    public static final String OUTPUT_RELATIVE_PATH = "jacoco/jacoco.exec";

    public static final String OUTPUT_XML_RELATIVE_PATH = "jacoco/jacoco.xml";

    public static final String OUTPUT_HTML_RELATIVE_PATH = "jacoco/html";  // this is a folder

    public static final String DEFAULT_VERSION = "0.8.11";

    private final ToolProvider toolProvider;

    private Path execFile;

    private Path classDir;

    private JkPathMatcher classDirFilter;

    private boolean htmlReport = true;

    private boolean xmlReport = true;

    private List<Path> sourceDirs = new LinkedList<>();

    private final List<String> agentOptions = new LinkedList<>();

    private final List<String> reportOptions = new LinkedList<>();

    private JkJacoco(ToolProvider toolProvider) {
        super();
        this.toolProvider = toolProvider;
    }

    /**
     * Returns the {@link JkJacoco} object relying on jacoco-agent and jacoco-cli hosted on repository.
     */
    public static JkJacoco ofVersion(JkDependencyResolver dependencyResolver,
                                     @JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent") String version) {
        return new JkJacoco(new RepoToolProvider(dependencyResolver, version));
    }

    /**
     * @see #ofVersion(JkDependencyResolver, String)
     */
    public static JkJacoco ofVersion(JkRepoSet repos,
                                     @JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent") String version) {
        return ofVersion(JkDependencyResolver.of(repos), version);
    }

    /**
     * @see #ofVersion(JkDependencyResolver, String)
     */
    public static JkJacoco ofVersion(@JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent") String version) {
        return ofVersion(JkRepo.ofMavenCentral().toSet(), version);
    }

    /**
     * Returns the {@link JkJacoco} object relying on jacoco-agent and jacoco-cli embedded in this plugin.
     */
    public static JkJacoco ofEmbedded() {
        return new JkJacoco(new EmbeddedToolProvider());
    }

    /**
     * Configures Jacoco settings in accordance to the specified project.
     * It basically sets the report locations and instructs where source code is lying.
     */
    public JkJacoco configureFor(JkProject project) {
        this.setExecFile(project.getOutputDir().resolve(OUTPUT_RELATIVE_PATH))
                .setClassDir(project.compilation.layout.getClassDirPath());
        if (xmlReport) {
            this.addReportOptions("--xml",
                    project.getOutputDir().resolve(OUTPUT_XML_RELATIVE_PATH).toString());
        }
        if (htmlReport) {
            this.addReportOptions("--html",
                    project.getOutputDir().resolve(OUTPUT_HTML_RELATIVE_PATH).toString());
        }
        List<Path> sourceDirs = project.compilation
                .layout.getSources().getRootDirsOrZipFiles().stream()
                .map(path -> path.isAbsolute() ? path : project.getBaseDir().resolve(path))
                .collect(Collectors.toList());
        this.setSources(sourceDirs);
        return this;
    }

    /**
     * Concise method for configuring Jacoco based on the specified project and applying the settings to the designated
     * project's testProcessor.
     */
    public JkJacoco configureForAndApplyTo(JkProject project) {
        configureFor(project).applyTo(project.testing.testProcessor);
        return this;
    }

    /**
     * Applies this Jacoco settings to the specified {@link JkTestProcessor}, in order
     * it runs with Jacoco.
     */
    public void applyTo(JkTestProcessor testProcessor) {
        JkUtilsAssert.state(execFile != null, "The exec file has not been specified.");
        testProcessor.preActions.append("", () -> {
            String agentOptions = agentOptions();
            JkJavaProcess process = JkUtilsObject.firstNonNull(testProcessor.getForkingProcess(),
                    JkJavaProcess.ofJava(JkTestProcessor.class.getName()));
            process.addAgent(toolProvider.getAgentJar(), agentOptions);
            JkLog.info("Instrumenting tests with Jacoco agent options : " + agentOptions);
            testProcessor.setForkingProcess(process);
            testProcessor.postActions.append(this::generateExport);
        });
    }

    /**
     *  @param htmlReport if true, produces an xml report.
     */
    public JkJacoco setHtmlReport(boolean htmlReport) {
        this.htmlReport = htmlReport;
        return this;
    }

    /**
     *  @param xmlReport if true, produces an xml report.
     */
    public JkJacoco setXmlReport(boolean xmlReport) {
        this.xmlReport = xmlReport;
        return this;
    }

    public JkJacoco setExecFile(Path destFile) {
        this.execFile = destFile;
        return this;
    }

    public JkJacoco addAgentOptions(String ...args) {
        agentOptions.addAll(Arrays.asList(args));
        return this;
    }

    /**
     * Necessary to produce XML report
     */
    public JkJacoco setClassDir(Path classDir) {
        this.classDir = classDir;
        return this;
    }

    public JkJacoco setClassDirFilter(JkPathMatcher pathMatcher) {
        this.classDirFilter = pathMatcher;
        return this;
    }

    public JkJacoco setClassDirFilter(String pathDirExcludes) {
        JkPathMatcher pathMatcher = JkPathMatcher.of(false, pathDirExcludes.split(","));
        this.classDirFilter = pathMatcher;
        return this;
    }

    /**
     * See https://www.jacoco.org/jacoco/trunk/doc/cli.html for report option
     */
    public JkJacoco addReportOptions(String ...args) {
        reportOptions.addAll(Arrays.asList(args));
        return this;
    }

    public JkJacoco setSources(List<Path> sourceDirs) {
        this.sourceDirs = sourceDirs;
        return this;
    }

    public List<String> getReportOptions() {
        return reportOptions;
    }

    /**
     * Generates XML and HTML reports from the exec report file.
     */
    public void generateExport() {
        JkLog.info("Jacoco internal report created at " + execFile.toAbsolutePath().normalize());
        if (!reportOptions.isEmpty()) {
            if (classDir == null) {
                JkLog.warn("No class dir specified. Cannot run jacoco report.");
                return;
            }
            if (!Files.exists(execFile)) {
                JkLog.warn("File " + execFile + " not found. Cannot run jacoco report.");
                return;
            }
            JkPathTree pathTree = null;
            if (classDirFilter != null) {
                pathTree = JkPathTree.of(classDir).withMatcher(classDirFilter);
            }
            List<String> args = new LinkedList<>();
            args.add("report");
            args.add(execFile.toString());
            if (classDirFilter == null) {
                args.add("--classfiles");
                args.add(classDir.toString());
            } else {
                pathTree.getFiles().forEach(file ->  {
                    args.add("--classfiles");
                    args.add(file.toString());
                });
            }
            for (Path sourceRoot : sourceDirs) {
                // args.add("--sourcefiles");
                // args.add(sourceRoot.toString());
            }
            args.add("--encoding");
            args.add("utf-8");
            args.addAll(reportOptions);
            if (!JkLog.isVerbose()) {
                args.add("--quiet");
            }
            JkLog.info("Generate Jacoco XML report with args " + args);

            JkJavaProcess.ofJavaJar(toolProvider.getCmdLineJar(), null)
                    .setFailOnError(true)
                    .setLogCommand(JkLog.isVerbose())
                    .addParams(args)
                    .exec();
        }
    }

    /**
     * The agent jar path, used to instrument tests
     */
    public Path getAgentJar() {
        return this.getToolProvider().getAgentJar();
    }

    /**
     * Exec file is the binary report file generated by Jacoco
     */
    public Path getExecFile() {
        return execFile;
    }

    private ToolProvider getToolProvider() {
        return toolProvider;
    }



    private JkPathTree pathTree() {
        JkUtilsAssert.state(classDir != null, "Class dir has not been specified.");
        JkPathTree result = JkPathTree.of(classDir);
        if (classDirFilter != null) {
            result = result.withMatcher(classDirFilter);
        }
        return result;
    }

    private String agentOptions() {
        String result = String.join(",", agentOptions);
        boolean hasDestFile = agentOptions.stream()
                .filter(option -> option.startsWith("destfile="))
                .findFirst().isPresent();
        if (!hasDestFile) {
            if (!JkUtilsString.isBlank(result)) {
                result = result + ",";
            }
            result = result + "destfile=" + JkUtilsPath.relativizeFromWorkingDir(execFile);
        }
        return result;
    }

    private interface ToolProvider {

        Path getAgentJar();

        Path getCmdLineJar();

    }

    public static class RepoToolProvider implements ToolProvider {

        JkDependencyResolver dependencyResolver;

        String version;

        RepoToolProvider(JkDependencyResolver dependencyResolver, String version) {
            this.dependencyResolver = dependencyResolver;
            this.version = version;
        }

        public Path getAgentJar() {
            return JkCoordinateFileProxy.of(dependencyResolver.getRepos(),
                    "org.jacoco:org.jacoco.agent:runtime:" + version).get();
        }

        public Path getCmdLineJar() {
            return JkCoordinateFileProxy.of(dependencyResolver.getRepos(),
                    "org.jacoco:org.jacoco.cli:nodeps:" + version).get();
        }
    }

    private static class EmbeddedToolProvider implements ToolProvider {

        private final Path agentJarFile;

        private final Path cliJarFile;

        EmbeddedToolProvider() {
            final URL agentJarUrl = JkJacoco.class.getResource("org.jacoco.agent-0.8.7-runtime.jar");
            agentJarFile = JkUtilsIO.copyUrlContentToCacheFile(agentJarUrl, System.out,
                    JkInternalEmbeddedClassloader.URL_CACHE_DIR);
            final URL cliJarUrl = JkJacoco.class.getResource("org.jacoco.cli-0.8.7-nodeps.jar");
            cliJarFile = JkUtilsIO.copyUrlContentToCacheFile(cliJarUrl, System.out,
                    JkInternalEmbeddedClassloader.URL_CACHE_DIR);
        }

        @Override
        public Path getAgentJar() {
            return agentJarFile;
        }

        @Override
        public Path getCmdLineJar() {
            return cliJarFile;
        }
    }




}
