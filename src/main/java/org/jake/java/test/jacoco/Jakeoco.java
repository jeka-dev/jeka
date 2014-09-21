package org.jake.java.test.jacoco;

import java.io.File;

import lombok.Data;

import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.java.JakeJavaProcess;
import org.jake.java.build.JakeBuildJava;
import org.jake.java.test.junit.JakeUnit;

@Data
public final class Jakeoco {

	private final File agent;

	private final boolean enabled;

	private final File destFile;

	private Jakeoco(File agent,boolean enabled, File destFile) {
		super();
		this.agent = agent;
		this.enabled = enabled;
		this.destFile = destFile;
	}

	public static Jakeoco of(File destFile) {
		return new Jakeoco(defaultAgentFile(), true, destFile);
	}

	public static Jakeoco of(JakeBuildJava jakeBuildJava) {
		final File agent = jakeBuildJava.baseDir("build/libs/jacoco-agent/jacocoagent.jar");
		final File agentFile = agent.exists() ? agent : defaultAgentFile();
		if (!agentFile.exists()) {
			throw new IllegalStateException("No jacocoagent.jar found neither in " + defaultAgentFile().getAbsolutePath()
					+ " nor in " + agent.getAbsolutePath() );
		}
		return new Jakeoco(agentFile, true, new File(jakeBuildJava.testReportDir(), "jacoco/jacoco.exec"));
	}

	public Jakeoco withAgent(File jacocoagent) {
		return new Jakeoco(jacocoagent, enabled, destFile);
	}

	public JakeUnit enhance(JakeUnit jakeUnit) {
		if (!enabled) {
			return jakeUnit;
		}
		if (jakeUnit.isForked()) {
			JakeJavaProcess process = jakeUnit.getFork();
			process = process.andAgent(destFile, options());
			return jakeUnit.fork(process);
		}
		final JakeJavaProcess process = JakeJavaProcess.of().andAgent(agent, options());
		return jakeUnit.forkKeepingSameClassPath(process).withPostAction(new Reporter());
	}

	private String options() {
		final StringBuilder builder = new StringBuilder();
		builder.append("destfile=").append(destFile.getAbsolutePath());
		return builder.toString();
	}

	private static File defaultAgentFile() {
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
