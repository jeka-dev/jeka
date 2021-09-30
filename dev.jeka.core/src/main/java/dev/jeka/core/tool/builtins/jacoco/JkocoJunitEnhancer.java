package dev.jeka.core.tool.builtins.jacoco;

import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsObject;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Enhancer to beforeOptionsInjected JkUnit such it performs Jacoco code coverage while it runs unit tests.
 *
 * Note : May sometime fall in this issue when running from IDE :
 * https://stackoverflow.com/questions/31720139/jacoco-code-coverage-report-generator-showing-error-classes-in-bundle-code-c
 */
public final class JkocoJunitEnhancer {

    private Path agent;

    private boolean enabled = true;

    private Path execFile;

    private Path classDir;

    private final List<String> agentOptions = new LinkedList<>();

    private final List<String> reportOptions = new LinkedList<>();

    private JkocoJunitEnhancer(Path agent, Path destFile) {
        super();
        this.agent = agent;
        this.execFile = destFile;
    }

    public static JkocoJunitEnhancer of(Path destFile) {
        final URL agentJarUrl = JkPluginJacoco.class.getResource("org.jacoco.agent-0.8.7-runtime.jar");
        final Path agentJarFile = JkUtilsIO.copyUrlContentToCacheFile(agentJarUrl, System.out, JkInternalClassloader.URL_CACHE_DIR);
        return new JkocoJunitEnhancer(agentJarFile, destFile);
    }

    public JkocoJunitEnhancer setAgent(Path jacocoagent) {
        this.agent = jacocoagent;
        return this;
    }

    public JkocoJunitEnhancer addAgentOptions(String ...args) {
        agentOptions.addAll(Arrays.asList(args));
        return this;
    }

    /**
     * Necessary to produce XML report
     */
    public JkocoJunitEnhancer setClassDir(Path classDir) {
        this.classDir = classDir;
        return this;
    }

    /**
     * See https://www.jacoco.org/jacoco/trunk/doc/cli.html for report option
     */
    public JkocoJunitEnhancer addReportOptions(String ...args) {
        reportOptions.addAll(Arrays.asList(args));
        return this;
    }

    public List<String> getReportOptions() {
        return reportOptions;
    }

    public JkocoJunitEnhancer enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void apply(JkTestProcessor testProcessor) {
        if (!enabled) {
            return;
        }
        testProcessor.getPreActions().append(() -> {
            JkJavaProcess process = JkUtilsObject.firstNonNull(testProcessor.getForkingProcess(), JkJavaProcess.of());
            process = process.andAgent(agent, agentOptions());
            testProcessor.setForkingProcess(process);
            testProcessor.getPostActions().append(new Reporter());
        });

    }

    private String agentOptions() {
        final StringBuilder builder = new StringBuilder();
        builder.append("destfile=").append(execFile.toAbsolutePath());
        if (Files.exists(execFile)) {
            builder.append(",append=true");
        }
        for (final String option : agentOptions) {
            builder.append(",").append(option);
        }
        return builder.toString();
    }

    private class Reporter implements Runnable {

        @Override
        public void run() {
            if (enabled) {
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
                    final URL cliJarUrl = JkPluginJacoco.class.getResource("org.jacoco.cli-0.8.7-nodeps.jar");
                    final Path cliJarFile = JkUtilsIO.copyUrlContentToCacheFile(cliJarUrl, System.out,
                            JkInternalClassloader.URL_CACHE_DIR);
                    List<String> args = new LinkedList<>();
                    args.add("report");
                    args.add(execFile.toAbsolutePath().toString());
                    args.add("--classfiles");
                    args.add(classDir.toString());
                    args.add("--encoding");
                    args.add("utf-8");
                    args.addAll(reportOptions);
                    JkLog.info("Generate Jacoco report using " + args);
                    JkJavaProcess.of().withPrintCommand(false).runJarSync(cliJarFile, args.toArray(new String[0]));
                }

            }

        }

    }

}
