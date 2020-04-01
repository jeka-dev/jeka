package dev.jeka.core.tool.builtins.jacoco;

import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsObject;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Enhancer to beforeOptionsInjected JkUnit such it performs Jacoco code coverage while it runs unit tests.
 */
public final class JkocoJunitEnhancer {

    private final Path agent;

    private final boolean enabled;

    private final Path destFile;

    private final List<String> options;

    private JkocoJunitEnhancer(Path agent, boolean enabled, Path destFile, List<String> options) {
        super();
        this.agent = agent;
        this.enabled = enabled;
        this.destFile = destFile;
        this.options = options;
    }

    public static JkocoJunitEnhancer of(Path destFile) {
        final URL url = JkPluginJacoco.class.getResource("jacocoagent.jar");
        final Path file = JkUtilsIO.copyUrlContentToCacheFile(url, System.out, JkInternalClassloader.URL_CACHE_DIR);
        return new JkocoJunitEnhancer(file, true, destFile, Collections.emptyList());
    }

    public JkocoJunitEnhancer withAgent(Path jacocoagent) {
        return new JkocoJunitEnhancer(jacocoagent, enabled, destFile, options);
    }

    public JkocoJunitEnhancer withOptions(List<String> options) {
        return new JkocoJunitEnhancer(agent, enabled, destFile, new LinkedList<>(options));
    }

    public JkocoJunitEnhancer enabled(boolean enabled) {
        return new JkocoJunitEnhancer(this.agent, enabled, destFile, options);
    }

    public void apply(JkTestProcessor testProcessor) {
        if (!enabled) {
            return;
        }
        JkJavaProcess process = JkUtilsObject.firstNonNull(testProcessor.getForkingProcess(), JkJavaProcess.of());
        process = process.andAgent(destFile, options());
        testProcessor.setForkingProcess(process);
        testProcessor.getPostActions().append(new Reporter());
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
