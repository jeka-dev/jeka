package org.jake.java.testing.jacoco;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.java.JakeJavaProcess;
import org.jake.java.testing.junit.JakeUnit;

public final class Jakeoco implements JakeUnit.Enhancer {

	private final File agent;

	private final boolean enabled;

	private final File destFile;

	private final List<String> options;

	private Jakeoco(File agent,boolean enabled, File destFile, List<String> options) {
		super();
		this.agent = agent;
		this.enabled = enabled;
		this.destFile = destFile;
		this.options = new LinkedList<String>();
	}

	public static Jakeoco of(File destFile) {
		return new Jakeoco(defaultAgentFile(), true, destFile, new LinkedList<String>());
	}

	public Jakeoco withAgent(File jacocoagent) {
		return new Jakeoco(jacocoagent, enabled, destFile, options);
	}

	/**
	 * Append some options to the returned <code>Jakeoco</code>.
	 * One option is to be considered as a <code>pair=value</code>.<br/>
	 * Example : <code>withOptions("dumponexit=true", "port=6301");</code>
	 */
	public Jakeoco withOptions(String ...options) {
		return new Jakeoco(agent, enabled, destFile, Arrays.asList(options));
	}

	public Jakeoco enabled(boolean enabled) {
		return new Jakeoco(this.agent, enabled, destFile, options);
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

	public static File defaultAgentFile() {
		return new File(JakeLocator.optionalLibsDir(), "jacocoagent.jar");
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
