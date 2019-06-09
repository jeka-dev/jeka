package dev.jeka.core.tool.builtins.jacoco;

import org.jerkar.api.java.JkUrlClassLoader;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.junit.JkUnit;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;

import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Enhancer to beforeOptionsInjected JkUnit such it performs Jacoco code coverage while it runs unit tests.
 */
public final class JkocoJunitEnhancer implements UnaryOperator<JkUnit> {

    private final Path agent;

    private final boolean enabled;

    private final Path destFile;

    private final List<String> options;

    private JkocoJunitEnhancer(Path agent, boolean enabled, Path destFile) {
        super();
        this.agent = agent;
        this.enabled = enabled;
        this.destFile = destFile;
        this.options = new LinkedList<>();
    }

    public static JkocoJunitEnhancer of(Path destFile) {
        final URL url = JkPluginJacoco.class.getResource("jacocoagent.jar");
        PrintStream outputStream = JkLog.verbosity() == JkLog.Verbosity.VERBOSE ? new PrintStream(JkLog.getOutputStream()) : null;
        final Path file = JkUtilsIO.copyUrlContentToCacheFile(url, outputStream, JkUrlClassLoader.getUrlCacheDir());
        return new JkocoJunitEnhancer(file, true, destFile);
    }

    public JkocoJunitEnhancer withAgent(Path jacocoagent) {
        return new JkocoJunitEnhancer(jacocoagent, enabled, destFile);
    }

    public JkocoJunitEnhancer enabled(boolean enabled) {
        return new JkocoJunitEnhancer(this.agent, enabled, destFile);
    }

    @Override
    public JkUnit apply(JkUnit jkUnit) {
        if (!enabled) {
            return jkUnit;
        }
        if (jkUnit.isForked()) {
            JkJavaProcess process = jkUnit.getForkedProcess();
            process = process.andAgent(destFile, options());
            return jkUnit.withForking(process);
        }
        final JkJavaProcess process = JkJavaProcess.of().andAgent(agent, options());
        return jkUnit.withForking(process).withPostAction(new Reporter());
    }

    private String options() {
        final StringBuilder builder = new StringBuilder();
        builder.append("destfile=").append(destFile.toAbsolutePath());
        if (Files.exists(destFile)) {
            builder.append(",append=true");
        }
        for (final String option : options) {
            builder.append(",").append(option);
        }
        return builder.toString();
    }

    private class Reporter implements Runnable {

        @Override
        public void run() {
            if (enabled) {
                JkLog.info("Jacoco report created at " + destFile.toAbsolutePath());
            }

        }

    }

}
