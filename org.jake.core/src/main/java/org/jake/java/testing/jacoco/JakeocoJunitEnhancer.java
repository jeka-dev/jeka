package org.jake.java.testing.jacoco;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLog;
import org.jake.java.JakeJavaProcess;
import org.jake.java.testing.junit.JakeUnit;
import org.jake.java.testing.junit.JakeUnit.Enhancer;
import org.jake.utils.JakeUtilsIO;

public final class JakeocoJunitEnhancer implements Enhancer {

	private final File agent;

	private final boolean enabled;

	private final File destFile;

	private final List<String> options;

	private JakeocoJunitEnhancer(File agent ,boolean enabled, File destFile, List<String> options) {
		super();
		this.agent = agent;
		this.enabled = enabled;
		this.destFile = destFile;
		this.options = new LinkedList<String>();
	}

	public static JakeocoJunitEnhancer of(File destFile, File projectDir) {
		final URL url = JakeocoJakeJavaBuildPlugin.class.getResource("jacocoagent.jar");
		final File file = JakeUtilsIO.getFileFromUrl(url, new File(projectDir, "/build/output/temp"));
		return new JakeocoJunitEnhancer(file, true, destFile, new LinkedList<String>());
	}

	public JakeocoJunitEnhancer withAgent(File jacocoagent) {
		return new JakeocoJunitEnhancer(jacocoagent, enabled, destFile, options);
	}

	/**
	 * Append some options to the returned <code>Jakeoco</code>.
	 * One option is to be considered as a <code>pair=value</code>.<br/>
	 * Example : <code>withOptions("dumponexit=true", "port=6301");</code>
	 */
	public JakeocoJunitEnhancer withOptions(String ...options) {
		return new JakeocoJunitEnhancer(agent, enabled, destFile, Arrays.asList(options));
	}

	public JakeocoJunitEnhancer enabled(boolean enabled) {
		return new JakeocoJunitEnhancer(this.agent, enabled, destFile, options);
	}

	@Override
	public JakeUnit enhance(JakeUnit jakeUnit) {
		if (!enabled) {
			return jakeUnit;
		}
		if (jakeUnit.forked()) {
			JakeJavaProcess process = jakeUnit.processFork();
			process = process.andAgent(destFile, options());
			return jakeUnit.fork(process);
		}
		final JakeJavaProcess process = JakeJavaProcess.of().andAgent(agent, options());
		return jakeUnit.forkKeepingSameClassPath(process).withPostAction(new Reporter());
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
				JakeLog.info("Jacoco report created at " + destFile.getAbsolutePath());
			}

		}

	}

}
