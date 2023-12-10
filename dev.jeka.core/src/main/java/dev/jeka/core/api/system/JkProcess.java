package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
public class JkProcess<T extends JkProcess> implements Runnable {

    private static final Path CURRENT_JAVA_DIR = Paths.get(System.getProperty("java.home")).resolve("bin");

    private String command;

    private List<String> parameters;

    private Map<String, String> env = new HashMap();

    private Path workingDir;

    private boolean failOnError = true;

    private boolean logCommand;

    private boolean logOutputWithLogDecorator = true;

    private boolean destroyAtJvmShutdown;

    private boolean redirectErrorStream = true;
    
    // By default, streams are redirected with gobbler mechanism to not bypass JkLog decorator.
    private boolean inheritIO = false;

    protected JkProcess() {}

    protected JkProcess(String command, String... parameters) {
        this.command = command;
        this.parameters = new LinkedList<>(Arrays.asList(parameters));
    }

    protected JkProcess(JkProcess other) {
        this.command = other.command;
        this.parameters = new LinkedList(other.parameters);
        this.env = new HashMap(other.env);
        this.failOnError = other.failOnError;
        this.logCommand = other.logCommand;
        this.logOutputWithLogDecorator = other.logOutputWithLogDecorator;
        this.workingDir = other.workingDir;
        this.destroyAtJvmShutdown = other.destroyAtJvmShutdown;
    }

    /**
     * Defines a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess<JkProcess> of(String command, String... parameters) {
        return new JkProcess(command, parameters);
    }

    public static JkProcess<JkProcess> ofCmdLine(String commandLine) {
        String[] items = JkUtilsString.translateCommandline(commandLine);
        JkUtilsAssert.argument(items.length > 0, "Cannot accept empty command line.");
        String cmd = items[0];
        List<String> args = new LinkedList<>(Arrays.asList(items));
        args.remove(0);
        return new JkProcess(cmd, args.toArray(new String[0]));
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

    public T copy()  {
        return (T) new JkProcess<T>(this);
    }

    /**
     * Specify the command to execute
     */
    public T setCommand(String command) {
        this.command = command;
        return (T) this;
    }

    public T setDestroyAtJvmShutdown(boolean destroy) {
        this.destroyAtJvmShutdown = destroy;
        return (T) this;
    }

    /**
     * Adds specified parameters to the command line
     */
    public T addParams(String... parameters) {
        return addParams(Arrays.asList(parameters));
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

    public T setEnv(String name, String value) {
        this.env.put(name, value);
        return (T) this;
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
     * Set if the output streams of the process must bbe logged keeping the JkLog decorator.
     * <p>
     * This is generally desirable, as the output will honor the indentation and visibility
     * set by JeKa log decorator. <br/>
     * This implies a small extra resource costs that is acceptable in most of the situations.
     * <br/>
     * Nevertheless, if you want bypass this mechanism, you can specify to not log using this mechanism.
     * In this case, you may set up explicitly a mean to get the output streams. One solution
     * consists in invoking <code>setInheritIO(true)</code>
     *
     * Initial value is <code>true</code>..
     *
     */
    public  T setLogOutput(boolean logOutputWithLogDecorator) {
        this.logOutputWithLogDecorator = logOutputWithLogDecorator;
        return (T) this;
    }

    /**
     * Makes the process use the same streams as its parent. <p>
     * This can not be used in conjunction of {@link #logOutputWithLogDecorator}, so this one is disabled forcibly when invoking this method.
     * <br/>
     * Initial value is <code>false</code>
     *
     * @see  {@link ProcessBuilder#inheritIO()}.
     */
    public T setInheritIO(boolean value) {
        if (value) {
            this.logOutputWithLogDecorator = false;
        }
        this.inheritIO = value;
        return (T) this;
    }

    /**
     * Adds a param -lv=[DecoratorStyle] matching the current one.
     */
    public T inheritJkLogOptions() {
        if (JkLog.getDecoratorStyle() != null) {
            addParams("-ls=" + JkLog.getDecoratorStyle().name());
        }
        if (JkLog.isVerbose()) {
            addParams("-lv");
        }
        if(!JkLog.isAcceptAnimation()) {
            addParams("-lna");
        }
        return (T) this;
    }

    /**
     * Same as {@link ProcessBuilder#redirectErrorStream(boolean)}. The difference is that the initial
     * value is <code>true</code> for {@link JkProcess}
     */
    public T redirectErrorStream(boolean value) {
        this.redirectErrorStream = value;
        return (T) this;
    }

    /**
     * Same as {@link #exec(String...)} () but only effective if the specified condition is <code>true</code>.
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
        if (extraParams.length == 1) {
            extraParams = JkUtilsString.translateCommandline(extraParams[0]);
        }
        return exec(false, process -> {}, extraParams).exitCode;
    }

    /**
     * Same as {@link #exec(String...)} but the provided process consumer will be called right after
     * the process is started. It can be used to get th process pid.
     */
    public int exec(Consumer<Process> processConsumer, String ... extraParams) {
        if (extraParams.length == 1) {
            extraParams = JkUtilsString.translateCommandline(extraParams[0]);
        }
        return exec(false, processConsumer, extraParams).exitCode;
    }

    public List<String> execAndReturnOutput(String ... extraParams) {
        Result result = exec(true, process -> {}, extraParams);
        if (result.output.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(result.output.split("\\r?\n"));
    }

    private Result exec(boolean collectOutput, Consumer<Process> processConsumer, String ... extraParams) {
        JkUtilsAssert.state(!JkUtilsString.isBlank(command), "No command has been specified");
        final List<String> commands = new LinkedList<>();
        commands.add(this.command);
        commands.addAll(parameters);
        commands.addAll(Arrays.asList(extraParams));
        commands.removeAll(Collections.singleton(null));
        customizeCommand(commands);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream collectOs = collectOutput ? byteArrayOutputStream : JkUtilsIO.nopOutputStream();
        if (logCommand) {
            String workingDirName = this.workingDir == null ? "" : this.workingDir + ">";
            JkLog.startTask("Start program : " + workingDirName + commands);
        }
        int exitCode = runProcess(commands,processConsumer, collectOs);
        if (logCommand) {
            JkLog.endTask();
        }
        Result out = new Result();
        out.exitCode = exitCode;
        out.output = collectOutput ? byteArrayOutputStream.toString() : null;
        return out;
    }

    protected void customizeCommand(List<String> commands) {
    }

    private int runProcess(List<String> commands, Consumer<Process> processConsumer, OutputStream collectOs) {
        final ProcessBuilder processBuilder = processBuilder(commands);
        final Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (destroyAtJvmShutdown) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }));
        }
        processConsumer.accept(process);

        // Initialize stream globber so output stream of subprocess does not bybass decorators 
        // set in place in JkLog
        JkUtilsIO.JkStreamGobbler outputStreamGobbler = null;
        JkUtilsIO.JkStreamGobbler errorStreamGobbler = null;

        // TODO find a better criteria to turn off the globber
        // Apparently, the globber is needed to return result
        if (!inheritIO) {
            OutputStream consoleOutputStream = logOutputWithLogDecorator ? JkLog.getOutPrintStream() : JkUtilsIO.nopOutputStream();
            OutputStream consoleErrStream = logOutputWithLogDecorator ? JkLog.getErrPrintStream() : JkUtilsIO.nopOutputStream();
            outputStreamGobbler = JkUtilsIO.newStreamGobbler(process.getInputStream(), consoleOutputStream, collectOs);
            errorStreamGobbler = JkUtilsIO.newStreamGobbler(process.getErrorStream(), consoleErrStream, collectOs);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        
        if (!inheritIO) {
            outputStreamGobbler.join();
            errorStreamGobbler.join();
        }
        if (exitCode != 0 && failOnError) {
            throw new IllegalStateException("Process " + String.join(" ", ellipsed(commands))
                    + "\nhas returned with error code " + exitCode);
        }
        return exitCode;
    }

    private static List<String> ellipsed(List<String> options) {
        if (JkLog.isVerbose()) {
            return options;
        }
        return options.stream()
                .map(option -> JkUtilsString.ellipse(option, 120))
                .collect(Collectors.toList());
    }

    private static class Result {
        int exitCode;
        String output;
    }

    private ProcessBuilder processBuilder(List<String> command) {
        if (inheritIO && logOutputWithLogDecorator) {
            throw new IllegalStateException("inheritIO and logOutput can not be used in conjunction. " +
                    "You have to choose between one of them.");
        }
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(redirectErrorStream);
        if (inheritIO) {
            builder.inheritIO();
        }
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

    public List<String> getParams() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Returns <code>true</code> if this process must throw an execption if the underlying process returns
     * code different from 0.
     */
    public boolean isFailOnError() {
        return failOnError;
    }

    @Override
    public String toString() {
        return this.command + " " + String.join(" ", parameters);
    }

}
