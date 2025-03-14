/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The JkProcess class represents a process that can be executed on the system.
 * It provides various methods for configuring and executing the process.
 */
public abstract class JkAbstractProcess<T extends JkAbstractProcess> implements Runnable {

    protected static final Path CURRENT_JAVA_DIR = Paths.get(System.getProperty("java.home")).resolve("bin");

    // Parameters that form the command line to run the process.
    private List<String> processParams = new LinkedList<>();

    private Map<String, String> env = new HashMap<>();

    private Path workingDir;

    private boolean failOnError = true;

    private boolean logCommand;

    private boolean logWithJekaDecorator = true;

    private boolean destroyAtJvmShutdown;

    private boolean redirectErrorStream;

    private boolean collectStdout;

    private boolean collectStderr;

    // By default, streams are redirected with gobbler mechanism to not bypass JkLog decorator.
    private boolean inheritIO = false;

    protected JkAbstractProcess() {}

    protected JkAbstractProcess(JkAbstractProcess<?> other) {
        this.processParams = new LinkedList<>(other.processParams);
        this.env = new HashMap<>(other.env);
        this.failOnError = other.failOnError;
        this.logCommand = other.logCommand;
        this.logWithJekaDecorator = other.logWithJekaDecorator;
        this.workingDir = other.workingDir;
        this.destroyAtJvmShutdown = other.destroyAtJvmShutdown;
        this.inheritIO = other.inheritIO;
        this.redirectErrorStream = other.redirectErrorStream;
        this.collectStdout = other.collectStdout;
        this.collectStderr = other.collectStderr;
    }

    protected T copy() {
        return null;
    }

    /**
     * Specify the command to execute
     */
    public T setParamAt(int index, String parameter) {
        this.processParams.remove(index);
        this.processParams.add(index, parameter);
        return (T) this;
    }

    /**
     * Sets the flag to indicate whether the std out of the process should be collected.
     * <p>
     * This is mandatory to collect output if we want to get the get the {@link JkProcResult#getStdout()}
     * after process execution.
     * <p>
     * Initial value is <code>false</code>.
     */
    public T setCollectStdout(boolean collectStdout) {
        this.collectStdout = collectStdout;
        return (T) this;
    }

    /**
     * Sets the flag to indicate whether the std err of the process should be collected.
     * <p>
     * This is mandatory to collect output if we want to get the get the {@link JkProcResult#getStdout()}
     * after process execution.
     * <p>
     * Initial value is <code>false</code>.
     */
    public T setCollectStderr(boolean collectStderr) {
        this.collectStderr = collectStderr;
        return (T) this;
    }

    /**
     * Sets the flag to destroy the process at JVM shutdown.
     */
    public T setDestroyAtJvmShutdown(boolean destroy) {
        this.destroyAtJvmShutdown = destroy;
        return (T) this;
    }

    /**
     * Adds the specified parameters as a space separated args to the command line.
     * The string will be parsed in an array of parameters.
     *
     * @param cmdLine the parameters as a string (e.g '-X -e run').
     * @param tokens If command line contains tokens as '%s', they are replaced by these tokens.
     *
     */
    public T addParamsAsCmdLine(String cmdLine, Object ...tokens) {
        return addParams(JkUtilsString.parseCommandline(String.format(cmdLine, tokens)));
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
        processParams.remove(parameter);
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
     * Adds the specified parameters to the command line.
     *
     * @see  #addParams(String...)
     */
    public T addParams(Collection<String> parameters) {
        List<String> sanitizedParams = sanitized(parameters);
        List<String> params = new LinkedList<>(this.processParams);
        params.addAll(sanitizedParams);
        this.processParams = params;
        return (T) this;
    }

    /**
     * Adds the specified parameters to the command line at the beginning of the list.
     * Any null values in the parameters collection will be removed before adding.
     */
    public T addParamsAt(int index, Collection<String> parameters) {
        List<String> sanitizedParams = sanitized(parameters);
        List<String> params = new LinkedList<>(this.processParams);
        params.addAll(index, sanitizedParams);
        this.processParams = params;
        return (T) this;
    }

    /**
     * Adds the specified parameters to the command line at the beginning of the list.
     * @see #addParamsAt(int, Collection)
     */
    public T addParamsAt(int index, String ...parameters) {
        return addParamsAt(index, Arrays.asList(parameters));
    }

    /**
     * Sets the specified working directory to launch the process.
     */
    public T setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        return (T) this;
    }

    /**
     * Sets the specified working directory to launch the process.
     *
     * @see #setWorkingDir(Path) .
     */
    public T setWorkingDir(String workingDir) {
        return setWorkingDir(Paths.get(workingDir));
    }

    /**
     * Sets the value of the environment variable with the specified name.
     *
     * @param name the name of the environment variable
     * @param value the value to be assigned to the environment variable
     */
    public T setEnv(String name, String value) {
        this.env.put(name, value);
        return (T) this;
    }

    /**
     * Specifies if the running process should throw an Exception in case process returns with a
     * code different to 0.<p>
     * Initial value is 'true'.
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
     * Set if the output streams of the process must be logged keeping the JkLog decorator.
     * <p>
     * This may be desirable, as the output will honor the indentation and visibility
     * set by JeKa log decorator.
     * This implies a small extra resource costs that is acceptable in most of the situations.
     * <p>
     * Be aware that the stdout and stderr of the sub-process will be mixed
     * in the {@link Process#getInputStream()} as specified in this method Javadoc.
     * <p>
     * Nevertheless, if you want to bypass this mechanism, you can specify to not using it.
     * In this case, you may set up explicitly a mean to get the output streams. One solution
     * consists in invoking <code>setInheritIO(true)</code>
     * <p>
     * Initial value is <code>true</code>..
     */
    public T setLogWithJekaDecorator(boolean logOutputWithLogDecorator) {
        this.logWithJekaDecorator = logOutputWithLogDecorator;
        return (T) this;
    }

    /**
     * Makes the process use the same streams as its parent. <p>
     * This can not be used in conjunction of {@link #logWithJekaDecorator}, so this one is disabled forcibly when invoking this method.
     * <br/>
     * Initial value is <code>false</code>
     *
     * @see ProcessBuilder#inheritIO()
     */
    public T setInheritIO(boolean value) {
        if (value) {
            this.logWithJekaDecorator = false;
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
        if(!JkLog.isAnimationAccepted()) {
            addParams("-lna");
        }
        return (T) this;
    }

    /**
     * Same as {@link ProcessBuilder#redirectErrorStream(boolean)}.
     * <p>
     * Initial value is false.
     */
    public T redirectErrorStream(boolean value) {
        this.redirectErrorStream = value;
        return (T) this;
    }

    /**
     * Returns the working directory of this process.
     */
    public Path getWorkingDir() {
        return workingDir;
    }

    /**
     * Returns the command launched by  this process.
     */
    public String getParamAt(int index) {
        return this.processParams.get(index);
    }

    public List<String> getParams() {
        return Collections.unmodifiableList(processParams);
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
        int maxLength = 150;
        if (JkLog.isVerbose()) {
            return JkUtilsString.readableCommandAgs("", processParams);
        }
        String shortCommand = shortenCommand();
        return JkUtilsString.ellipse(shortCommand + " " + String.join(" ", processParams), maxLength);
    }

    /**
     * Modifies the command and its execution parameters.
     * <p>
     * This method provides a way for subclasses to adjust the process before it is run.
     */
    protected void customizeCommand() {
    }

    @Override
    public void run() {
        this.exec();
    }

    /**
     * Starts this process and wait for the process has finished prior
     * returning. The output of the created process will be redirected on the
     * current output.
     */
    public JkProcResult exec() {
        customizeCommand();
        if (logCommand) {
            String cmd = shortenCommand() +  " " + shortenArgs(100);
            JkLog.startTask("start-program >" + cmd);
            if (JkLog.isVerbose()) {
                printContextualInfo();
            }
        }
        if (inheritIO) {
            JkLog.getOutPrintStream().flush();
            JkLog.getErrPrintStream().flush();
        }

        ByteArrayOutputStream collectOutBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream collectErrBaos = new ByteArrayOutputStream();
        final OutputStream collectStdoutStream = collectStdout ? collectOutBaos : JkUtilsIO.nopOutputStream();
        final OutputStream collectStderrStream = collectStderr ? collectErrBaos : JkUtilsIO.nopOutputStream();
        int exitCode = runProcess(collectStdoutStream, collectStderrStream);
        if (logCommand) {
            JkLog.endTask();
        }
        return new JkProcResult(
                exitCode,
                collectStdout ? collectOutBaos.toByteArray() : null,
                collectStderr ? collectErrBaos.toByteArray() : null);
    }

    /**
     * Executes a command with the specified parameters and returns the current instance.
     *
     * This method uses a clone of the current instance's configuration to execute a command.
     *
     * @param commandParams The parameters to be added to the command being executed.
     * @return The current instance after executing the command.
     */
    public T execCmd(String ...commandParams) {
        this.copy().addParams(commandParams).exec();
        return (T) this;
    }

    /**
     * @see #execCmd(String...)
     */
    public T execCmdLine(String format, String... tokens) {
        String cmdLne = String.format(format, (Object[]) tokens);
        return execCmd(JkUtilsString.parseCommandline(cmdLne));
    }

    /**
     * Executes the process asynchronously and returns a {@link JkProcHandler} object
     * which can be used to interact with the running process.
     */
    public JkProcHandler execAsync() {
        customizeCommand();
        if (logCommand) {
            String cmd = shortenCommand() +  " " + shortenArgs(100);
            JkLog.info("start-async-program >" + cmd);
            if (JkLog.isVerbose()) {
                printContextualInfo();
            }
        }
        if (inheritIO) {
            JkLog.getOutPrintStream().flush();
            JkLog.getErrPrintStream().flush();
        }
        Process process = runProcessAsync();

        // Collect the output of sub-process if it has been required to.
        ByteArrayOutputStream baos = null;
        if (!inheritIO && this.collectStdout) {
            baos = new ByteArrayOutputStream();
            JkUtilsIO.newStreamGobbler(process, process.getInputStream(), JkUtilsIO.nopOutputStream(), baos);
        }

        return new JkProcHandler(process, baos);
    }

    private Process runProcessAsync() {
        final ProcessBuilder processBuilder = processBuilder(this.processParams);
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
        return process;
    }

    private int runProcess(OutputStream collectStdoutStream, OutputStream collectStderrStream) {
        final Process process = runProcessAsync();

        // Initialize stream globber so output stream of subprocess does not bybass decorators 
        // set in place in JkLog
        JkUtilsIO.JkStreamGobbler outputStreamGobbler = null;
        JkUtilsIO.JkStreamGobbler errorStreamGobbler = null;

        // TODO find a better criteria to turn off the globber
        // Apparently, the globber is needed to return result
        if (!inheritIO) {
            OutputStream consoleOutStream = logWithJekaDecorator ?
                    JkLog.getOutPrintStream() : JkUtilsIO.nopOutputStream();
            OutputStream consoleErrStream = logWithJekaDecorator ?
                    JkLog.getErrPrintStream() : JkUtilsIO.nopOutputStream();
            outputStreamGobbler = JkUtilsIO.newStreamGobbler(process, process.getInputStream(),
                    consoleOutStream, collectStdoutStream);
            errorStreamGobbler = JkUtilsIO.newStreamGobbler(process, process.getErrorStream(),
                    consoleErrStream, collectStderrStream);
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
            JkLog.error("Process exited with error code %s", exitCode);
            printContextualInfo();
            if (collectStdout) {
                ByteArrayOutputStream collectedStdout = (ByteArrayOutputStream) collectStdoutStream;
                JkLog.error("Std out was ===============================================");
                System.out.println(JkAnsi.of().fgBright(JkAnsi.Color.BLACK));
                JkUtilsIO.write(JkLog.getOutPrintStream(), collectedStdout.toByteArray());
                System.out.println(JkAnsi.of().reset());
                if (!collectStderr) {
                    JkLog.error("===========================================================");
                }
                JkLog.getOutPrintStream().flush();

            }
            if (collectStderr) {
                ByteArrayOutputStream collectedStdErr = (ByteArrayOutputStream) collectStderrStream;
                JkLog.error("Std err was ===============================================");
                System.out.println(JkAnsi.of().fgBright(JkAnsi.Color.BLACK));
                JkUtilsIO.write(JkLog.getOutPrintStream(), collectedStdErr.toByteArray());
                System.out.println(JkAnsi.of().reset());
                JkLog.error("===========================================================");
                JkLog.getErrPrintStream().flush();

            }

            throw new IllegalStateException("Process has returned with error code " + exitCode);
        }
        return exitCode;
    }

    private ProcessBuilder processBuilder(List<String> command) {
        if (inheritIO && logWithJekaDecorator) {
            throw new IllegalStateException("inheritIO and logWithJekaDecorator can not be used in conjunction. " +
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

    private static List<String> sanitized(Collection<String> params) {
        return params.stream()
                .filter(Objects::nonNull)
                .filter(param -> !param.isEmpty())
                .collect(Collectors.toList());
    }

    private String fullCmdLine() {
        return String.join(" ", processParams);
    }

    private String shortenArgs(int maxWidth) {
        if (processParams.size() == 1) {
            return "";
        }
        String singleLine = String.join(" ", processParams.subList(1, processParams.size()));
        return JkUtilsString.ellipse(singleLine, maxWidth);
    }

    private String shortenCommand() {
        Path path = Paths.get(processParams.get(0) );
        return path.getFileName().toString();
    }

    private void printContextualInfo() {
        String workingDirName = this.workingDir == null ? "." : workingDir.toString();
        JkLog.info("working dir   : %s", workingDirName);
        String cmdPath = processParams.isEmpty() ? "" : processParams.get(0);
        String cmdArgs = JkUtilsString.substringAfterFirst(fullCmdLine(), cmdPath).trim();
        String formattedCmdArgs = formatOptions(cmdArgs);
        JkLog.info("command path  : %s", cmdPath);
        JkLog.info("command args  : %s", formattedCmdArgs);
        JkLog.getOutPrintStream().flush();
    }

    private static String formatOptions(String optionLine) {
        List<String> options = Arrays.asList(JkUtilsString.parseCommandline(optionLine));
        return JkUtilsString.formatOptions(options);
    }

}
