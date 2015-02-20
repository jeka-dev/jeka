package org.jake.java.testing.jacoco;

import java.io.File;

import org.jake.JakeDoc;
import org.jake.JakeException;
import org.jake.JakeOption;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.testing.junit.JakeUnit;
import org.jake.java.testing.junit.JakeUnit.Enhancer;

@JakeDoc("Performs Jacoco code coverage analysing while junit is running.")
public class JakeocoJakeJavaBuildPlugin extends JakeJavaBuildPlugin {

	private static final String AGENT_RELATIVE_PATH= "build/libs/jacoco-agent/jacocoagent.jar";

	public static Enhancer enhancer(JakeJavaBuild jakeJavaBuild) {
		return enhancer(jakeJavaBuild, jakeJavaBuild.baseDir(AGENT_RELATIVE_PATH), false);
	}

	private Enhancer enhancer;

	@JakeOption("The path of the jacocoagent.jar file. By default, it is [project dir]/build/libs/jacoco-agent/jacocoagent.jar")
	private File jacocoAgent = new File(AGENT_RELATIVE_PATH);

	@Override
	public void configure(JakeJavaBuild jakeJavaBuild) {
		if (!jacocoAgent.isAbsolute()) {
			jacocoAgent = jakeJavaBuild.baseDir(AGENT_RELATIVE_PATH);
		}
		this.enhancer = enhancer(jakeJavaBuild, jacocoAgent, true);
	}

	@Override
	public JakeUnit enhance(JakeUnit jakeUnit) {
		return this.enhancer.enhance(jakeUnit);
	}

	private static Enhancer enhancer(JakeJavaBuild jakeJavaBuild, File agent, boolean failIfAgentNotExist) {
		if (failIfAgentNotExist && !agent.exists()) {
			throw new JakeException("No jacocoagent.jar found in " + agent.getAbsolutePath() );
		}
		final File agentFile = agent.exists() ? agent : JakeocoJunitEnhancer.defaultAgentFile();
		if (!agentFile.exists()) {
			throw new JakeException("No jacocoagent.jar found neither in "
					+ JakeocoJunitEnhancer.defaultAgentFile().getAbsolutePath()
					+ " nor in " + agent.getAbsolutePath() );
		}
		return JakeocoJunitEnhancer.of(
				new File(jakeJavaBuild.testReportDir(), "jacoco/jacoco.exec")).withAgent(agentFile);
	}





}
