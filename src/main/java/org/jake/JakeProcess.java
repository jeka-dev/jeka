package org.jake;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIO.StreamGobbler;
import org.jake.utils.JakeUtilsString;

/**
 * Offers fluent API to define and launch external process.
 * 
 * @author Jerome Angibaud
 */
public final class JakeProcess {

	private static final File CURRENT_JAVA_DIR = new File(System.getProperty("java.home"), "bin");

	private final String command;

	private final List<String> parameters;

	private final File workingDir;

	private JakeProcess(String command, List<String> parameters, File workingDir) {
		this.command = command;
		this.parameters = parameters;
		this.workingDir = workingDir;
	}

	/**
	 * Defines a <code>JakeProcess</code> using the specified command and parameters.
	 */
	public static JakeProcess of(String command, String... parameters) {
		return new JakeProcess(command, Arrays.asList(parameters), null);
	}

	/**
	 * Defines a <code>JakeProcess</code> using the specified tool of the JDK and parameters.
	 * An example of JDK tool is 'javac'.
	 */
	public static JakeProcess ofJavaTool(String javaTool, String... parameters) {
		File candidate = CURRENT_JAVA_DIR;
		final boolean exist = findTool(candidate, javaTool);
		if (!exist) {
			candidate = new File (CURRENT_JAVA_DIR.getParentFile().getParentFile(), "bin");
			if (!findTool(candidate, javaTool)) {
				throw new IllegalArgumentException("No tool " + javaTool + " found neither in " + CURRENT_JAVA_DIR.getAbsolutePath()
						+ " nor in " + candidate.getAbsolutePath());
			}
		}
		final String command = candidate.getAbsolutePath() + File.separator + javaTool;
		return of(command, parameters);
	}


	public JakeProcess andParameters(Collection<String> parameters) {
		final List<String> list = new ArrayList<String>(this.parameters);
		list.addAll(parameters);
		return new JakeProcess(command, list, workingDir);
	}

	public JakeProcess withParameters(String... parameters) {
		return new JakeProcess(command, Arrays.asList(parameters), workingDir);
	}

	public JakeProcess withWorkingDir(File workingDir) {
		return new JakeProcess(command, parameters, workingDir);
	}

	private ProcessBuilder processBuilder(List<String> command) {
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		if (this.workingDir != null) {
			builder.directory(workingDir);
		}
		return builder;
	}

	/**
	 * Starts this defined process and wait for the process has finished prior returning.
	 */
	public int startAndWaitFor() {
		final List<String> command = new LinkedList<String>();
		command.add(this.command);
		command.addAll(parameters);
		JakeLog.startln("Starting java program : " + command.toString());
		final int result;
		try {
			final ProcessBuilder processBuilder = processBuilder(command);
			if (workingDir != null) {
				processBuilder.directory(this.workingDir);
			}
			final Process process = processBuilder.start();
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
		JakeLog.done(" process exit with return code : " + result);
		return result;
	}

	private static boolean findTool(File dir, String name) {
		for (final File file : dir.listFiles()) {
			if (file.isDirectory()) {
				continue;
			}
			if (file.getName().equals(name)) {
				return true;
			}
			final String fileToolName = JakeUtilsString.substringBeforeLast(file.getName(), ".");
			if (fileToolName.equals(name)) {
				return true;
			}
		}
		return false;
	}




}
