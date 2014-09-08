package org.jake.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jake.JakeLog;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIO.StreamGobbler;
import org.jake.utils.JakeUtilsString;

public class JakeJavaProcess {

	private static final File CURRENT_JAVA_DIR = new File(System.getProperty("java.home"));

	private final Map<String, String> sytemProperties;

	private final File javaDir;

	private final JakeClasspath classpath;

	private final List<AgentLibAndOption> agents;

	private final Collection<String> options;

	private final File workingDir;

	private JakeJavaProcess(File javaDir, Map<String, String> sytemProperties, JakeClasspath classpath,
			List<AgentLibAndOption> agents, Collection<String> options, File workingDir) {
		super();
		this.javaDir = javaDir;
		this.sytemProperties = sytemProperties;
		this.classpath = classpath;
		this.agents = agents;
		this.options = options;
		this.workingDir = workingDir;
	}

	/**
	 * Initializes a <code>JakeJavaProcess</code> using the same JRE then the one currently running.
	 */
	public static JakeJavaProcess of() {
		return of(CURRENT_JAVA_DIR);
	}

	/**
	 * Initializes a <code>JakeJavaProcess</code> using the Java executable located in the specified directory.
	 */
	@SuppressWarnings("unchecked")
	public static JakeJavaProcess of(File javaDir) {
		return new JakeJavaProcess(javaDir, Collections.EMPTY_MAP, JakeClasspath.of(),
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
	}

	public JakeJavaProcess andAgent(File agentLib, String agentOption) {
		if (agentLib == null) {
			throw new NullPointerException("agentLib can't be null.");
		}
		if (agentLib.exists() && agentLib.exists() && agentLib.isFile()) {
			throw new IllegalArgumentException("aggentLib must be an existing file (not a directory).");
		}
		final List<AgentLibAndOption> list = new ArrayList<JakeJavaProcess.AgentLibAndOption>(this.agents);
		list.add(new AgentLibAndOption(agentLib.getAbsolutePath(), agentOption));
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, this.classpath,
				list, this.options, this.workingDir);
	}

	public JakeJavaProcess andAgent(File agentLib) {
		return andAgent(agentLib, null);
	}

	public JakeJavaProcess andOptions(Collection<String> options) {
		final List<String> list = new ArrayList<String>(this.options);
		list.addAll(options);
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, this.classpath,
				this.agents, list, this.workingDir);
	}

	public JakeJavaProcess andOptions(String... options) {
		return this.andOptions(Arrays.asList(options));
	}

	public JakeJavaProcess withWorkingDir(File workingDir) {
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, this.classpath,
				this.agents, this.options, workingDir);
	}

	public JakeJavaProcess withClasspath(JakeClasspath classpath) {
		if (classpath == null) {
			throw new NullPointerException("Classpath can't be null.");
		}
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, classpath,
				this.agents, this.options, this.workingDir);
	}

	private ProcessBuilder processBuilder(String command) {
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		if (this.workingDir != null) {
			builder.directory(workingDir);
		}
		return builder;
	}

	private StringBuilder runningJavaCommand() {
		return new StringBuilder(this.javaDir.getAbsolutePath()).append(File.separator).append("java");
	}

	private StringBuilder command() {
		return runningJavaCommand().append(options());
	}

	private StringBuilder arguments(String ...arguments) {
		final StringBuilder builder = new StringBuilder();
		for (final String argument : arguments) {
			builder.append(" ").append(argument);
		}
		return builder;
	}

	public int startAndWaitFor(String mainClassName, String ...arguments) {
		JakeLog.startAndNextLine("Starting java program on class " + mainClassName + " using args : " + Arrays.toString(arguments));
		final String command = command().append(" ").append(mainClassName).append(arguments(arguments)).toString();
		try {
			final Process process = processBuilder(command).start();
			final StreamGobbler outputStreamGobbler =
					JakeUtilsIO.newStreamGobbler(process.getInputStream(), JakeLog.getInfoWriter());
			final StreamGobbler errorStreamGobbler =
					JakeUtilsIO.newStreamGobbler(process.getErrorStream(), JakeLog.getErrorWriter());
			process.waitFor();
			outputStreamGobbler.stop();
			errorStreamGobbler.stop();
			return process.exitValue();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		} finally {
			JakeLog.done();
		}
	}

	private StringBuilder options() {
		final StringBuilder builder = new StringBuilder();
		if (classpath != null && !classpath.isEmpty()) {
			builder.append(" -cp \"").append(classpath.toString()).append("\"");
		}
		for (final AgentLibAndOption agentLibAndOption : agents) {
			builder.append(" -javaagent:").append(agentLibAndOption.lib);
			if (!JakeUtilsString.isBlank(agentLibAndOption.options)) {
				builder.append("=").append(agentLibAndOption.options);
			}
		}
		for (final String key : this.sytemProperties.keySet()) {
			final String value = this.sytemProperties.get(key);
			builder.append(" -D").append(key).append("=").append(value);
		}
		for (final String option : options) {
			builder.append(" ").append(option);
		}
		return builder;
	}


	private static class AgentLibAndOption {

		public final String lib;

		public final String options;

		public AgentLibAndOption(String lib, String options) {
			super();
			this.lib = lib;
			this.options = options;
		}
	}

}
