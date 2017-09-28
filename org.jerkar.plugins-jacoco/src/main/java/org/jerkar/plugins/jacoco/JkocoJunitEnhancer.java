package org.jerkar.plugins.jacoco;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;

/**
 * Enhancer to configure JkUnit such it performs Jacoco code coverage while it runs unit tests.
 */
public final class JkocoJunitEnhancer implements UnaryOperator<JkUnit> {

    private final File agent;

    private final boolean enabled;

    private final File destFile;

    private final List<String> options;

    private JkocoJunitEnhancer(File agent, boolean enabled, File destFile) {
        super();
        this.agent = agent;
        this.enabled = enabled;
        this.destFile = destFile;
        this.options = new LinkedList<>();
    }

    public static JkocoJunitEnhancer of(File destFile) {
        final URL url = JkPluginJacoco.class.getResource("jacocoagent.jar");
        final File file = JkUtilsIO.copyUrlContentToCacheFile(url, JkLog.infoStreamIfVerbose(),
                JkClassLoader.urlCacheDir());
        return new JkocoJunitEnhancer(file, true, destFile);
    }

    public JkocoJunitEnhancer withAgent(File jacocoagent) {
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
            JkJavaProcess process = jkUnit.forkedProcess();
            process = process.andAgent(destFile, options());
            return jkUnit.forked(process);
        }
        final JkJavaProcess process = JkJavaProcess.of().andAgent(agent, options());
        return jkUnit.forked(process).withPostAction(new Reporter());
    }

    private String options() {
        final StringBuilder builder = new StringBuilder();
        builder.append("destfile=").append(destFile.getAbsolutePath());
        if (destFile.exists()) {
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
                JkLog.info("Jacoco report created at " + destFile.getAbsolutePath());
            }

        }

    }

}
