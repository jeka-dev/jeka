package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final boolean logOutput;

    private JkProcess(String command, List<String> parameters, Path workingDir, boolean failOnError, boolean logCommand
            , boolean logOutput) {
        this.command = command;
        this.parameters = parameters;
        this.workingDir = workingDir;
        this.failOnError = failOnError;
        this.logCommand = logCommand;
        this.logOutput = logOutput;
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess of(String command, String... parameters) {
        return new JkProcess(command, Arrays.asList(parameters), null, false, false, true);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess ofWinOrUx(String windowsCommand, String unixCommand,
            String... parameters) {
        final String cmd = JkUtilsSystem.IS_WINDOWS ? windowsCommand : unixCommand;
        return new JkProcess(cmd, Arrays.asList(parameters), null, false, false, true);
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
    public JkProcess andParams(String... parameters) {
        return andParams(Arrays.asList(parameters));
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but minus the
     * specified parameter.
     */
    public JkProcess minusParam(String parameter) {
        final List<String> list = new LinkedList<>(parameters);
        list.remove(parameter);
        return withParams(list.toArray(new String[0]));
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified extra parameters if the conditional is <code>true</code>.
     * Returns <code>this</code> otherwise.
     */
    public JkProcess andParamsIf(boolean conditional, String... parameters) {
        if (conditional) {
            return andParams(parameters);
        }
        return this;
    }

    /**
     * @see #andParams(String...)
     */
    public JkProcess andParams(Collection<String> parameters) {
        final List<String> list = new ArrayList<>(this.parameters);
        list.addAll(parameters);
        return new JkProcess(command, list, workingDir, failOnError, logCommand, logOutput);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the
     * specified parameters in place of this parameters. Contrary to
     * {@link #andParams(String...)}, this method replaces this parameters
     * by the specified ones (not adding).
     */
    public JkProcess withParams(String... parameters) {
        return new JkProcess(command, Arrays.asList(parameters), workingDir, failOnError, logCommand, logOutput);
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
        return new JkProcess(command, parameters, workingDir, failOnError, logCommand, logOutput);
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
        return new JkProcess(command, parameters, workingDir, fail, logCommand, logOutput);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the specified logging command behavior.
     * If parameter is <code>true</code>, a process execution will be wrapped in a log showing start and end of
     * the execution showing details about the command to be executed and execution duration.
     */
    public  JkProcess withLogCommand(boolean logCommand) {
        return new JkProcess(command, parameters, workingDir, failOnError, logCommand, logOutput);
    }

    /**
     * Returns a <code>JkProcess</code> identical to this one but with the specified logging output behavior.
     * If parameter is <code>true</code>, a process output will be redirected to JkLog.
     */
    public  JkProcess withLogOutput(boolean logOutput) {
        return new JkProcess(command, parameters, workingDir, failOnError, logCommand, logOutput);
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
        return runSync(false).exitCode;
    }

    public List<String> runAndReturnOutputAsLines() {
        Result result = runSync(true);
        if (result.output.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(result.output.split("\\r?\n"));
    }

    private Result runSync(boolean collectOutput) {
        final List<String> commands = new LinkedList<>();
        commands.add(this.command);
        commands.addAll(parameters);
        final AtomicInteger exitCode = new AtomicInteger();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream collectOs = collectOutput ? byteArrayOutputStream : JkUtilsIO.nopOuputStream();
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
            OutputStream consoleOutputStream = logOutput ? JkLog.getOutputStream() : JkUtilsIO.nopOuputStream();
            OutputStream consoleErrStream = logOutput ? JkLog.getErrorStream() : JkUtilsIO.nopOuputStream();
            final JkUtilsIO.JkStreamGobbler outputStreamGobbler = JkUtilsIO.newStreamGobbler(
                        process.getInputStream(), consoleOutputStream, collectOs);
                final JkUtilsIO.JkStreamGobbler errorStreamGobbler = JkUtilsIO.newStreamGobbler(
                        process.getErrorStream(), consoleErrStream, collectOs);
            try {
                exitCode.set(process.waitFor());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            outputStreamGobbler.join();
            errorStreamGobbler.join();
            if (exitCode.get() != 0 && failOnError) {
                throw new IllegalStateException("Process " + commands + " has returned with error code " + exitCode);
            }
        };
        if (logCommand) {
            String workingDirName = this.workingDir == null ? "" : this.workingDir.toString() +  ">";
            JkLog.startTask("Starting program : " + workingDirName + commands.toString());
            runnable.run();
            JkLog.endTask();
        } else {
            runnable.run();
        }
        Result out = new Result();
        out.exitCode = exitCode.get();
        out.output = collectOutput ? new String(byteArrayOutputStream.toByteArray()) : null;
        return out;
    }

    private static class Result {
        int exitCode;
        String output;
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
    public Path getWorkingDir() {
        return workingDir;
    }

    /**
     * Returns the command launched by this process.
     */
    public String getCommand() {
        return this.command;
    }

    /**
     * Returns <code>true</code> if this process must throw an execption if the underlying process returns
     * code different than 0.
     */
    public boolean isFailOnError() {
        return failOnError;
    }

    @Override
    public String toString() {
        return this.command + " " + JkUtilsString.join(parameters, " ");
    }

}
