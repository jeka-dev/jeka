package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

    private final ToolProvider toolProvider;

    private Path execFile;

    private Path classDir;

    private JkPathMatcher classDirFilter;

    private final List<String> agentOptions = new LinkedList<>();

    private final List<String> reportOptions = new LinkedList<>();

    private JkJacoco(ToolProvider toolProvider) {
        super();
        this.toolProvider = toolProvider;
    }

    /**
     * Returns the {@link JkJacoco} object relying on jacoco-agent and jacoco-cli embedded in this plugin.
     */
    public static JkJacoco ofEmbedded() {
        return new JkJacoco(new EmbeddedToolProvider());
    }

    /**
     * Returns the {@link JkJacoco} object relying on jacoco-agent and jacoco-cli hosted on repository.
     */
    public static JkJacoco ofManaged(JkDependencyResolver dependencyResolver, String version) {
        return new JkJacoco(new RepoToolProvider(dependencyResolver, version));
    }

    public static JkJacoco ofManaged(JkRepoSet repos, String version) {
        return ofManaged(JkDependencyResolver.of().addRepos(repos), version);
    }

    public static JkJacoco ofManaged(String version) {
        return ofManaged(JkRepo.ofMavenCentral().toSet(), version);
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

    /**
     * See https://www.jacoco.org/jacoco/trunk/doc/cli.html for report option
     */
    public JkJacoco addReportOptions(String ...args) {
        reportOptions.addAll(Arrays.asList(args));
        return this;
    }

    public List<String> getReportOptions() {
        return reportOptions;
    }

    public void configure(JkTestProcessor testProcessor) {
        JkUtilsAssert.state(execFile != null, "The exec file has not been specified.");
        testProcessor.getPreActions().append(() -> {
            String agentOptions = agentOptions();
            JkJavaProcess process = JkUtilsObject.firstNonNull(testProcessor.getForkingProcess(),
                    JkJavaProcess.ofJava(JkTestProcessor.class.getName()));
            process.addAgent(toolProvider.getAgentJar(), agentOptions);
            JkLog.info("Instrumenting tests with Jacoco agent " + agentOptions);
            testProcessor.setForkingProcess(process);
            testProcessor.getPostActions().append(new Reporter());
        });

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

    private class Reporter implements Runnable {

        @Override
        public void run() {
            JkLog.info("Jacoco internal report created at " + execFile.toAbsolutePath());
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
                args.add("--encoding");
                args.add("utf-8");
                args.addAll(reportOptions);
                if (!JkLog.isVerbose()) {
                    args.add("--quiet");
                }
                JkLog.info("Generate Jacoco report");
                JkJavaProcess.ofJavaJar(toolProvider.getCmdLineJar(), null)
                        .setFailOnError(true)
                        .setLogCommand(JkLog.isVerbose())
                        .addParams(args)
                        .exec();
            }
        }
    }

    private interface ToolProvider {

        Path getAgentJar();

        Path getCmdLineJar();

    }

    private static class RepoToolProvider implements ToolProvider {

        JkDependencyResolver dependencyResolver;

        String version;

        RepoToolProvider(JkDependencyResolver dependencyResolver, String version) {
            this.dependencyResolver = dependencyResolver;
            this.version = version;
        }

        public Path getAgentJar() {
            return dependencyResolver.resolve(JkDependencySet.of("org.jacoco:org.jacoco.agent:runtime:" + version))
                    .getFiles().getEntries().get(0);
        }

        public Path getCmdLineJar() {
            return dependencyResolver.resolve(JkDependencySet.of()
                    .and("org.jacoco:org.jacoco.cli:nodeps:" + version, JkTransitivity.NONE))
                    .getFiles().getEntries().get(0);
        }

    }

    private static class EmbeddedToolProvider implements ToolProvider {

        private Path agentJarFile;

        private Path cliJarFile;

        EmbeddedToolProvider() {
            final URL agentJarUrl = JkPluginJacoco.class.getResource("org.jacoco.agent-0.8.7-runtime.jar");
            agentJarFile = JkUtilsIO.copyUrlContentToCacheFile(agentJarUrl, System.out,
                    JkInternalClassloader.URL_CACHE_DIR);
            final URL cliJarUrl = JkPluginJacoco.class.getResource("org.jacoco.cli-0.8.7-nodeps.jar");
            cliJarFile = JkUtilsIO.copyUrlContentToCacheFile(cliJarUrl, System.out,
                    JkInternalClassloader.URL_CACHE_DIR);
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
