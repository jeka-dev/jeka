package org.jerkar.api.system;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIO.StreamGobbler;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;

/**
 * Provides fluent API to define and launch external process.
 * <p>
 * Parameters of the command are passed as array (and not as single string
 * representing several parameter separated with whitespace).<br/>
 * So for example, if you want to create a Maven process, then you should write
 * 
 * <pre>
 * <code>JkProcess.of("mvn", "clean", "install")</code>
 * </pre>
 * 
 * instead of
 * 
 * <pre>
 * <code>JkProcess.of("mvn", "clean install")</code>
 * </pre>
 * 
 * or
 * 
 * <pre>
 * <code>JkProcess.of("mvn clean install")</code>
 * </pre>
 * 
 * .
 *
 * @author Jerome Angibaud
 */
public final class JkProcess implements Runnable {

    private static final File CURRENT_JAVA_DIR = new File(System.getProperty("java.home"), "bin");

    private final String command;

    private final List<String> parameters;

    private final File workingDir;

    private final boolean failOnError;

    private JkProcess(String command, List<String> parameters, File workingDir, boolean failOnError) {
	this.command = command;
	this.parameters = parameters;
	this.workingDir = workingDir;
	this.failOnError = failOnError;
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess of(String command, String... parameters) {
	return new JkProcess(command, Arrays.asList(parameters), null, false);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess ofWinOrUx(String windowsCommand, String unixCommand, String... parameters) {
	final String cmd = JkUtilsSystem.IS_WINDOWS ? windowsCommand : unixCommand;
	return new JkProcess(cmd, Arrays.asList(parameters), null, false);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified tool of the JDK and
     * parameters. An example of JDK tool is 'javac'.
     */
    public static JkProcess ofJavaTool(String javaTool, String... parameters) {
	File candidate = CURRENT_JAVA_DIR;
	final boolean exist = findTool(candidate, javaTool);
	if (!exist) {
	    candidate = new File(CURRENT_JAVA_DIR.getParentFile().getParentFile(), "bin");
	    if (!findTool(candidate, javaTool)) {
		throw new IllegalArgumentException("No tool " + javaTool + " found neither in "
			+ CURRENT_JAVA_DIR.getAbsolutePath() + " nor in " + candidate.getAbsolutePath());
	    }
	}
	final String command = candidate.getAbsolutePath() + File.separator + javaTool;
	return of(command, parameters);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified extra parameters.
     */
    public JkProcess andParameters(String... parameters) {
	return andParameters(Arrays.asList(parameters));
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but minus the
     * specified parameter.
     */
    public JkProcess minusParameter(String parameter) {
	final List<String> list = new LinkedList<String>(parameters);
	list.remove(parameter);
	return withParameters(list.toArray(new String[0]));
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified extra parameters if the conditional is <code>true</code>.
     * Returns <code>this</code> otherwise.
     */
    public JkProcess andParametersIf(boolean conditional, String... parameters) {
	if (conditional) {
	    return andParameters(parameters);
	}
	return this;
    }

    /**
     * @see #andParameters(String...)
     */
    public JkProcess andParameters(Collection<String> parameters) {
	final List<String> list = new ArrayList<String>(this.parameters);
	list.addAll(parameters);
	return new JkProcess(command, list, workingDir, failOnError);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified parameters in place of this parameters. Contrary to
     * {@link #andParameters(String...)}, this method replaces this parameters
     * by the specified ones (not adding).
     */
    public JkProcess withParameters(String... parameters) {
	return new JkProcess(command, Arrays.asList(parameters), workingDir, failOnError);
    }

    /**
     * Same as {@link #withParameters(String...)} but only effective if the
     * specified conditional is true.
     */
    public JkProcess withParametersIf(boolean conditional, String... parameters) {
	if (conditional) {
	    return this.withParameters(parameters);
	}
	return this;
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but using the
     * specified directory as the working directory.
     */
    public JkProcess withWorkingDir(File workingDir) {
	return new JkProcess(command, parameters, workingDir, failOnError);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified behavior if the the underlying process does not exit with 0
     * code. In case of fail flag is <code>true</code> and the underlying
     * process exit with a non 0 value, the {@link #runSync()} method witll
     * throw a {@link IllegalStateException}.
     */
    public JkProcess failOnError(boolean fail) {
	return new JkProcess(command, parameters, workingDir, fail);
    }

    /**
     * Starts this process and wait for the process has finished prior
     * returning. The output of the created process will be redirected on the
     * current output.
     */
    public int runSync() {
	final List<String> command = new LinkedList<String>();
	command.add(this.command);
	command.addAll(parameters);
	JkLog.startln("Starting program : " + command.toString());
	final int result;
	try {
	    final ProcessBuilder processBuilder = processBuilder(command);
	    if (workingDir != null) {
		processBuilder.directory(this.workingDir);
	    }
	    final Process process = processBuilder.start();
	    final StreamGobbler outputStreamGobbler = JkUtilsIO.newStreamGobbler(process.getInputStream(),
		    JkLog.infoStream());
	    final StreamGobbler errorStreamGobbler = JkUtilsIO.newStreamGobbler(process.getErrorStream(),
		    JkLog.warnStream());
	    process.waitFor();
	    outputStreamGobbler.stop();
	    errorStreamGobbler.stop();
	    result = process.exitValue();
	    if (result != 0 && failOnError) {
		throw new IllegalStateException("The process has returned with error code " + result);
	    }
	} catch (final Exception e) {
	    throw new RuntimeException(e);
	}
	JkLog.done(" process exit with return code : " + result);
	return result;
    }

    private ProcessBuilder processBuilder(List<String> command) {
	final ProcessBuilder builder = new ProcessBuilder(command);
	builder.redirectErrorStream(true);
	if (this.workingDir != null) {
	    builder.directory(workingDir);
	}
	return builder;
    }

    private static boolean findTool(File dir, String name) {
	for (final File file : dir.listFiles()) {
	    if (file.isDirectory()) {
		continue;
	    }
	    if (file.getName().equals(name)) {
		return true;
	    }
	    final String fileToolName = JkUtilsString.substringBeforeLast(file.getName(), ".");
	    if (fileToolName.equals(name)) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public void run() {
	this.runSync();
    }

}
