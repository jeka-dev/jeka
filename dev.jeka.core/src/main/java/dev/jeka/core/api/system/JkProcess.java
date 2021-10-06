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
 * @author Jerome Angibaud
 */
public class JkProcess<T extends JkProcess> implements Runnable, Cloneable {

    private static final Path CURRENT_JAVA_DIR = Paths.get(System.getProperty("java.home")).resolve("bin");

    private String command;

    private List<String> parameters;

    private Map<String, String> env = new HashMap();

    private Path workingDir;

    private boolean failOnError;

    private boolean logCommand;

    private boolean logOutput = true;

    protected JkProcess(String command, String... parameters) {
        this.command = command;
        this.parameters = new LinkedList<>(Arrays.asList(parameters));
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess<JkProcess> of(String command, String... parameters) {
        return new JkProcess(command, parameters);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess<JkProcess> ofWinOrUx(String windowsCommand, String unixCommand,
            String... parameters) {
        final String cmd = JkUtilsSystem.IS_WINDOWS ? windowsCommand : unixCommand;
        return new JkProcess(cmd, parameters);
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

    @Override
    public T clone()  {
        try {
            JkProcess clone = (JkProcess) super.clone();
            clone.parameters = new LinkedList(parameters);
            clone.env = new HashMap(this.env);
            return (T) clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Specify the command to execute
     */
    public T setCommand(String command) {
        this.command = command;
        return (T) this;
    }

    /**
     * Adds specified parameters to the command line
     */
    public T addParams(String... parameters) {
        return (T) addParams(Arrays.asList(parameters));
    }

    /**
     * Removes specified parameter to the command line
     */
    public T removeParam(String parameter) {
        parameters.remove(parameter);
        return (T) this;
    }

    /**
     * Adds specified parameters to the command line if the specified condition is true.
     */
    public T addParamsIf(boolean condition, String... parameters) {
        if (condition) {
            return addParams(parameters);
        }
        return (T) this;
    }

    /**
     * @see #addParams(String...)
     */
    public T addParams(Collection<String> parameters) {
        List<String> params = new LinkedList<>(parameters);
        params.removeAll(Collections.singleton(null));
        this.parameters.addAll(params);
        return (T) this;
    }

    public T addParamsFirst(Collection<String> parameters) {
        List<String> params = new LinkedList<>(parameters);
        params.removeAll(Collections.singleton(null));
        this.parameters.addAll(0, params);
        return (T) this;
    }

    public T addParamsFirst(String ...parameters) {
        return addParamsFirst(Arrays.asList(parameters));
    }

    /**
     * Sets the specified working directory to launch the process.
     */
    public T setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        return (T) this;
    }

    public void setEnv(String name, String value) {
        this.env.put(name, value);
    }

    /**
     * @see #setWorkingDir(Path) .
     */
    public T setWorkingDir(String workingDir) {
        return setWorkingDir(Paths.get(workingDir));
    }

    /**
     * Specify if the running process should throw a Java Excption in case process returns with a
     * code different to 0.
     */
    public T setFailOnError(boolean fail) {
        this.failOnError = fail;
        return (T) this;
    }

    /**
     * If true, the command line will be outputed in the console
     */
    public  T setLogCommand(boolean logCommand) {
        this.logCommand = logCommand;
        return (T) this;
    }

    /**
     * If logOutput parameter is <code>true</code>, the process output will be redirected to JkLog.
     */
    public  T setLogOutput(boolean logOutput) {
        this.logOutput = logOutput;
        return (T) this;
    }

    /**
     * Same as {@link #exec(String...)} ()} but only effective if the specified condition is <code>true</code>.
     */
    public void execIf(boolean condition, String ... extraParams) {
        if (condition) {
            exec();
        }
    }

    /**
     * Starts this process and wait for the process has finished prior
     * returning. The output of the created process will be redirected on the
     * current output.
     */
    public int exec(String ... extraParams) {
        return exec(false, extraParams).exitCode;
    }

    public List<String> execAndReturnOutput(String ... extraParams) {
        Result result = exec(true, extraParams);
        if (result.output.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(result.output.split("\\r?\n"));
    }

    private Result exec(boolean collectOutput, String ... extraParams) {
        final List<String> commands = new LinkedList<>();
        commands.add(this.command);
        commands.addAll(parameters);
        commands.addAll(Arrays.asList(extraParams));
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
            JkLog.startTask("Start program : " + workingDirName + commands.toString());
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
        builder.environment().putAll(env);
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
        this.exec();
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
        return this.command + " " + String.join(" ", parameters);
    }

}
