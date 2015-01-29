package org.jake.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.JakeClasspath;
import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIO.StreamGobbler;
import org.jake.utils.JakeUtilsString;

/**
 * 
 * @author Djeang
 *
 */
public final class JakeJavaProcess {

	private static final File CURRENT_JAVA_DIR = new File(System.getProperty("java.home"), "bin");

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
			throw new IllegalArgumentException("Classpath can't be null.");
		}
		return new JakeJavaProcess(this.javaDir, this.sytemProperties, classpath,
				this.agents, this.options, this.workingDir);
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


	private ProcessBuilder processBuilder(List<String> command) {
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
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
		command.add(runningJavaCommand());
		command.addAll(options());
		command.add(mainClassName);
		command.addAll(Arrays.asList(arguments));
		JakeLog.startln("Starting java program : " + command.toString());
		final int result;
		try {
			final Process process = processBuilder(command).start();
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

	private List<String> options() {
		final List<String> list = new LinkedList<String>();
		if (classpath != null && !classpath.isEmpty()) {
			list.add("-cp");
			list.add(classpath.toString());
		}
		for (final AgentLibAndOption agentLibAndOption : agents) {
			final StringBuilder builder = new StringBuilder("-javaagent:").append(agentLibAndOption.lib);
			if (!JakeUtilsString.isBlank(agentLibAndOption.options)) {
				builder.append("="+agentLibAndOption.options);
			}
			list.add(builder.toString());
		}
		for (final String key : this.sytemProperties.keySet()) {
			final String value = this.sytemProperties.get(key);
			list.add("-D"+key+"="+value);
		}
		for (final String option : options) {
			list.add(option);
		}
		return list;
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
