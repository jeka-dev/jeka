package org.jake.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.JakeClasspath;
import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIO.StreamGobbler;
import org.jake.utils.JakeUtilsString;
import org.jake.utils.JakeUtilsSystem;

/**
 * Offer fluent interface for launching Java processes.
 * 
 * @author Jerome Angibaud
 */
public final class JakeJavaProcess {

	private static final File CURRENT_JAVA_DIR = new File(System.getProperty("java.home"), "bin");

	private final Map<String, String> sytemProperties;

	private final File javaDir;

	private final JakeClasspath classpath;

	private final List<AgentLibAndOption> agents;

	private final Collection<String> options;

	private final File workingDir;

	private final Map<String, String> environment;

	private JakeJavaProcess(File javaDir, Map<String, String> sytemProperties, JakeClasspath classpath,
			List<AgentLibAndOption> agents, Collection<String> options, File workingDir, Map<String, String> environment) {
		super();
		this.javaDir = javaDir;
		this.sytemProperties = sytemProperties;
		this.classpath = classpath;
		this.agents = agents;
		this.options = options;
		this.workingDir = workingDir;
		this.environment = environment;
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
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, Collections.EMPTY_MAP);
	}

	public JakeJavaProcess andAgent(File agentLib, String agentOption) {
		if (agentLib == null) {
			throw new IllegalArgumentException("agentLib can't be null.");
		}
		if (!agentLib.exists()) {
			throw new IllegalArgumentException("aggentLib " + agentLib.getAbsolutePath() + " not found.");
		}
		if (!agentLib.isFile()) {
			throw new IllegalArgumentException("aggentLib " + agentLib.getAbsolutePath() + " is a directory, should be a file.");
		}
		final List<AgentLibAndOption> list = new ArrayList<JakeJavaProcess.AgentLibAndOption>(this.agents);
		list.add(new AgentLibAndOption(agentLib.getAbsolutePath(), agentOption));
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, this.classpath,
				list, this.options, this.workingDir, this.environment);
	}

	public JakeJavaProcess andAgent(File agentLib) {
		return andAgent(agentLib, null);
	}

	public JakeJavaProcess andOptions(Collection<String> options) {
		final List<String> list = new ArrayList<String>(this.options);
		list.addAll(options);
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, this.classpath,
				this.agents, list, this.workingDir, this.environment);
	}

	public JakeJavaProcess andOptions(String... options) {
		return this.andOptions(Arrays.asList(options));
	}

	public JakeJavaProcess withWorkingDir(File workingDir) {
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, this.classpath,
				this.agents, this.options, workingDir, this.environment);
	}

	public JakeJavaProcess withClasspath(JakeClasspath classpath) {
		if (classpath == null) {
			throw new IllegalArgumentException("Classpath can't be null.");
		}
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, classpath,
				this.agents, this.options, this.workingDir, this.environment);
	}

	public JakeJavaProcess withClasspath(File ...files) {
		return withClasspath(JakeClasspath.of(files));
	}

	public JakeJavaProcess andClasspath(JakeClasspath classpath) {
		return withClasspath(this.classpath.and(classpath));
	}

	public JakeJavaProcess andClasspath(File ...files) {
		return withClasspath(this.classpath.and(files));
	}


	private ProcessBuilder processBuilder(List<String> command, Map<String, String> env) {
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		builder.environment().putAll(env);
		if (this.workingDir != null) {
			builder.directory(workingDir);
		}
		return builder;
	}

	private String runningJavaCommand() {
		return this.javaDir.getAbsolutePath()+ File.separator + "java";
	}

	public void startAndWaitFor(String mainClassName, String ...arguments) {
		final List<String> command = new LinkedList<String>();
		final OptionAndEnv optionAndEnv = optionsAndEnv();
		command.add(runningJavaCommand());
		command.addAll(optionAndEnv.options);
		command.add(mainClassName);
		command.addAll(Arrays.asList(arguments));
		JakeLog.startln("Starting java program : " + command.toString());
		final int result;
		try {
			final Process process = processBuilder(command, optionAndEnv.env).start();

			final StreamGobbler outputStreamGobbler =
					JakeUtilsIO.newStreamGobbler(process.getInputStream(), JakeLog.infoStream());
			final StreamGobbler errorStreamGobbler =
					JakeUtilsIO.newStreamGobbler(process.getErrorStream(), JakeLog.warnStream());
			process.waitFor();
			outputStreamGobbler.stop();
			errorStreamGobbler.stop();
			result = process.exitValue();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		if (result != 0) {
			throw new JakeException("Process terminated in error : exit value = " + result + ".");
		}
		JakeLog.done();
	}

	private OptionAndEnv optionsAndEnv() {
		final List<String> options = new LinkedList<String>();
		final Map<String, String> env = new HashMap<String, String>();
		if (classpath != null && !classpath.isEmpty()) {
			final String classpathString = classpath.toString();
			if (JakeUtilsSystem.IS_WINDOWS && classpathString.length() > 7500) {
				JakeLog.warn("classpath too long, classpath will be passed using CLASSPATH env variable.");
				env.put("CLASSPATH", classpathString);
			} else {
				options.add("-cp");
				options.add(classpath.toString());
			}
		}
		for (final AgentLibAndOption agentLibAndOption : agents) {
			final StringBuilder builder = new StringBuilder("-javaagent:").append(agentLibAndOption.lib);
			if (!JakeUtilsString.isBlank(agentLibAndOption.options)) {
				builder.append("="+agentLibAndOption.options);
			}
			options.add(builder.toString());
		}
		for (final String key : this.sytemProperties.keySet()) {
			final String value = this.sytemProperties.get(key);
			options.add("-D"+key+"="+value);
		}
		for (final String option : options) {
			options.add(option);
		}
		return new OptionAndEnv(options, env);
	}

	private static final class OptionAndEnv {

		public final List<String> options;
		public final Map<String, String> env;

		private OptionAndEnv(List<String> options, Map<String, String> env) {
			super();
			this.options = options;
			this.env = env;
		}

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

	public JakeClasspath classpath() {
		return classpath;

	}

}
