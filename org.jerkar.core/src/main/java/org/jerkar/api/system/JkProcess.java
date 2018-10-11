package org.jerkar.api.system;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsPath;
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
 * <code>JkProcess.of("mvn", "deleteArtifacts", "install")</code>
 * </pre>
 *
 * instead of
 *
 * <pre>
 * <code>JkProcess.of("mvn", "deleteArtifacts install")</code>
 * </pre>
 *
 * or
 *
 * <pre>
 * <code>JkProcess.of("mvn deleteArtifacts install")</code>
 * </pre>
 *
 * .
 *
 * @author Jerome Angibaud
 */
public final class JkProcess implements Runnable {

    private static final Path CURRENT_JAVA_DIR = Paths.get(System.getProperty("java.home")).resolve("bin");

    private final String command;

    private final List<String> parameters;

    private final Path workingDir;

    private final boolean failOnError;

    private final boolean logCommand;

    private JkProcess(String command, List<String> parameters, Path workingDir, boolean failOnError, boolean logCommand) {
        this.command = command;
        this.parameters = parameters;
        this.workingDir = workingDir;
        this.failOnError = failOnError;
        this.logCommand = logCommand;
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess of(String command, String... parameters) {
        return new JkProcess(command, Arrays.asList(parameters), null, false, false);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess ofWinOrUx(String windowsCommand, String unixCommand,
            String... parameters) {
        final String cmd = JkUtilsSystem.IS_WINDOWS ? windowsCommand : unixCommand;
        return new JkProcess(cmd, Arrays.asList(parameters), null, false, false);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified tool of the JDK and
     * parameters. An example of JDK tool is 'javac'.
     */
    public static JkProcess ofJavaTool(String javaTool, String... parameters) {
        Path candidate = CURRENT_JAVA_DIR;
        final boolean exist = findTool(candidate, javaTool);
        if (!exist) {
            candidate = CURRENT_JAVA_DIR.getParent().getParent().resolve("bin");
            if (!findTool(candidate, javaTool)) {
                throw new IllegalArgumentException("No tool " + javaTool + " found neither in "
                        + CURRENT_JAVA_DIR + " nor in "
                        + candidate);
            }
        }
        final String command = candidate.toAbsolutePath().normalize().resolve(javaTool).toString();
        return of(command, parameters);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified extra parameters.
     */
    public JkProcess withExtraParams(String... parameters) {
        return withExtraParams(Arrays.asList(parameters));
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but minus the
     * specified parameter.
     */
    public JkProcess withoutParam(String parameter) {
        final List<String> list = new LinkedList<>(parameters);
        list.remove(parameter);
        return withParams(list.toArray(new String[0]));
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified extra parameters if the conditional is <code>true</code>.
     * Returns <code>this</code> otherwise.
     */
    public JkProcess withExtraParamsIf(boolean conditional, String... parameters) {
        if (conditional) {
            return withExtraParams(parameters);
        }
        return this;
    }

    /**
     * @see #withExtraParams(String...)
     */
    public JkProcess withExtraParams(Collection<String> parameters) {
        final List<String> list = new ArrayList<>(this.parameters);
        list.addAll(parameters);
        return new JkProcess(command, list, workingDir, failOnError, logCommand);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified parameters in place of this parameters. Contrary to
     * {@link #withExtraParams(String...)}, this method replaces this parameters
     * by the specified ones (not adding).
     */
    public JkProcess withParams(String... parameters) {
        return new JkProcess(command, Arrays.asList(parameters), workingDir, failOnError, logCommand);
    }

    /**
     * Same as {@link #withParams(String...)} but only effective if the
     * specified conditional is true.
     */
    public JkProcess withParamsIf(boolean condition, String... parameters) {
        if (condition) {
            return this.withParams(parameters);
        }
        return this;
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but using the
     * specified directory as the working directory.
     */
    public JkProcess withWorkingDir(Path workingDir) {
        return new JkProcess(command, parameters, workingDir, failOnError, logCommand);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but using the
     * specified directory as the working directory.
     */
    public JkProcess withWorkingDir(String workingDir) {
        return withWorkingDir(Paths.get(workingDir));
    }


    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified behavior if the the underlying process does not exit with 0
     * code. In case of fail flag is <code>true</code> and the underlying
     * process exit with a non 0 value, the {@link #runSync()} method witll
     * throw a {@link IllegalStateException}.
     */
    public JkProcess withFailOnError(boolean fail) {
        return new JkProcess(command, parameters, workingDir, fail, logCommand);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the specified logging behavior.
     * If parameter is <code>true</code>, a process execution will be wrapped in a log showing start and end of
     * the execution showing details about the command to be executed and execution duration.
     */
    public  JkProcess withLogCommand(boolean logCommand) {
        return new JkProcess(command, parameters, workingDir, failOnError, logCommand);
    }

    /**
     * Same as {@link #runSync()} but only effective if the specified condition is <code>true</code>.
     */
    public void runSyncIf(boolean condition) {
        if (condition) {
            runSync();
        }
    }

    /**
     * Starts this process and wait for the process has finished prior
     * returning. The output of the created process will be redirected on the
     * current output.
     */
    public int runSync() {
        final List<String> commands = new LinkedList<>();
        commands.add(this.command);
        commands.addAll(parameters);
        final AtomicInteger result = new AtomicInteger();
        final Runnable runnable = () -> {

                final ProcessBuilder processBuilder = processBuilder(commands);
                if (workingDir != null) {
                    processBuilder.directory(this.workingDir.toAbsolutePath().normalize().toFile());
                }
            final Process process;
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            final JkUtilsIO.StreamGobbler outputStreamGobbler = JkUtilsIO.newStreamGobbler(
                        process.getInputStream(), JkLog.stream());
                final JkUtilsIO.StreamGobbler errorStreamGobbler = JkUtilsIO.newStreamGobbler(
                        process.getErrorStream(), JkLog.errorStream());
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            outputStreamGobbler.stop();
                errorStreamGobbler.stop();
                if (result.get() != 0 && failOnError) {
                    throw new IllegalStateException("The process has returned with error code "
                            + result);
                }

        };
        if (logCommand) {
            JkLog.execute("Starting program : " + commands.toString(), runnable);
        } else {
            runnable.run();
        }
        return result.get();
    }

    private ProcessBuilder processBuilder(List<String> command) {
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        if (this.workingDir != null) {
            builder.directory(workingDir.toAbsolutePath().normalize().toFile());
        }
        return builder;
    }

    private static boolean findTool(Path dir, String name) {
        if (!Files.exists(dir)) {
            return false;
        }
        for (final Path file : JkUtilsPath.listDirectChildren(dir)) {
            if (Files.isDirectory(file)) {
                continue;
            }
            if (file.getFileName().toString().equals(name)) {
                return true;
            }
            final String fileToolName = JkUtilsString.substringBeforeLast(file.getFileName().toString(), ".");
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

    /**
     * Returns the working directory of this process.
     */
    public Path workingDir() {
        return workingDir;
    }

    /**
     * Returns the command launched by this process.
     */
    public String getCommand() {
        return this.command;
    }

    @Override
    public String toString() {
        return this.command + " " + JkUtilsString.join(parameters, " ");
    }

}
