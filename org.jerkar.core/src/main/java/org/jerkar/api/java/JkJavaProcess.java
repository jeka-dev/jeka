package org.jerkar.api.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIO.StreamGobbler;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;

/**
 * Offers fluent interface for launching Java processes.
 *
 * @author Jerome Angibaud
 */
public final class JkJavaProcess {

    private static final File CURRENT_JAVA_DIR = new File(System.getProperty("java.home"), "bin");

    private final Map<String, String> sytemProperties;

    private final File javaDir;

    private final JkClasspath classpath;

    private final List<AgentLibAndOption> agents;

    private final Collection<String> options;

    private final File workingDir;

    private final Map<String, String> environment;

    private JkJavaProcess(File javaDir, Map<String, String> sytemProperties, JkClasspath classpath,
            List<AgentLibAndOption> agents, Collection<String> options, File workingDir,
            Map<String, String> environment) {
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
     * Initializes a <code>JkJavaProcess</code> using the same JRE then the one
     * currently running.
     */
    public static JkJavaProcess of() {
        return ofJavaHome(CURRENT_JAVA_DIR);
    }

    /**
     * Initializes a <code>JkJavaProcess</code> using the Java executable
     * located in the specified directory.
     */
    @SuppressWarnings("unchecked")
    public static JkJavaProcess ofJavaHome(File javaDir) {
        return new JkJavaProcess(javaDir, Collections.EMPTY_MAP, JkClasspath.of(),
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, Collections.EMPTY_MAP);
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but augmented with the
     * specified agent library and option.
     */
    public JkJavaProcess andAgent(File agentLib, String agentOption) {
        if (agentLib == null) {
            throw new IllegalArgumentException("agentLib can't be null.");
        }
        if (!agentLib.exists()) {
            throw new IllegalArgumentException("agentLib " + agentLib.getAbsolutePath()
            + " not found.");
        }
        if (!agentLib.isFile()) {
            throw new IllegalArgumentException("agentLib " + agentLib.getAbsolutePath()
            + " is a directory, should be a file.");
        }
        final List<AgentLibAndOption> list = new ArrayList<>(
                this.agents);
        list.add(new AgentLibAndOption(agentLib.getAbsolutePath(), agentOption));
        return new JkJavaProcess(this.javaDir, this.sytemProperties, this.classpath, list,
                this.options, this.workingDir, this.environment);
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but augnmented with the
     * specified agent library.
     */
    public JkJavaProcess andAgent(File agentLib) {
        return andAgent(agentLib, null);
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but with the
     * specified java options.
     */
    public JkJavaProcess andOptions(Collection<String> options) {
        final List<String> list = new ArrayList<>(this.options);
        list.addAll(options);
        return new JkJavaProcess(this.javaDir, this.sytemProperties, this.classpath, this.agents,
                list, this.workingDir, this.environment);
    }

    /**
     * Same as {@link #andOptions(Collection)} but effective only if the specified condition
     * is <code>true</code>.
     */
    public JkJavaProcess andOptionsIf(boolean condition, String... options) {
        if (condition) {
            return andOptions(options);
        }
        return this;
    }

    /**
     * Same as {@link #andOptions(Collection)}.
     */
    public JkJavaProcess andOptions(String... options) {
        return this.andOptions(Arrays.asList(options));
    }

    /**
     * Takes the specified command line as is and add it to the process command
     * line. Example of command line is <i>-Xms2G -Xmx2G</i>.
     */
    public JkJavaProcess andCommandLine(String commandLine) {
        if (JkUtilsString.isBlank(commandLine)) {
            return this;
        }
        return this.andOptions(JkUtilsString.translateCommandline(commandLine));
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but using the specified
     * working dir.
     */
    public JkJavaProcess withWorkingDir(File workingDir) {
        return new JkJavaProcess(this.javaDir, this.sytemProperties, this.classpath, this.agents,
                this.options, workingDir, this.environment);
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but using the specified
     * classpath.
     */
    public JkJavaProcess withClasspath(Iterable<File> classpath) {
        if (classpath == null) {
            throw new IllegalArgumentException("Classpath can't be null.");
        }
        final JkClasspath jkClasspath;
        if (classpath instanceof JkClasspath) {
            jkClasspath = (JkClasspath) classpath;
        } else {
            jkClasspath = JkClasspath.of(classpath);
        }
        return new JkJavaProcess(this.javaDir, this.sytemProperties, jkClasspath, this.agents,
                this.options, this.workingDir, this.environment);
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but using the specified
     * classpath.
     */
    public JkJavaProcess withClasspath(File... files) {
        return withClasspath(JkClasspath.of(files));
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but augmenting this
     * classpath with the specified one.
     */
    public JkJavaProcess andClasspath(JkClasspath classpath) {
        return withClasspath(this.classpath.and(classpath));
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but augmenting this
     * classpath with the specified one.
     */
    public JkJavaProcess andClasspath(File... files) {
        return andClasspath(Arrays.asList(files));
    }

    /**
     * Returns a {@link JkJavaProcess} identical to this one but augmenting this
     * classpath with the specified one.
     */
    public JkJavaProcess andClasspath(Iterable<File> files) {
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
        return this.javaDir.getAbsolutePath() + File.separator + "java";
    }

    /**
     * Runs the specified jar file and wait for termination.
     */
    public void runJarSync(File jar, String... arguments) {
        runClassOrJarSync(null, jar, arguments);
    }

    /**
     * Runs the specified class and wait for termination. The class has to be on this classpath.
     */
    public void runClassSync(String mainClassName, String... arguments) {
        runClassOrJarSync(mainClassName, null, arguments);
    }

    private void runClassOrJarSync(String mainClassName, File jar, String... arguments) {
        JkUtilsAssert.isTrue(jar != null || mainClassName != null,
                "main class name and jar can't be both null while launching a Java process, please set at least one of them.");
        final List<String> command = new LinkedList<>();
        final OptionAndEnv optionAndEnv = optionsAndEnv();
        command.add(runningJavaCommand());
        command.addAll(optionAndEnv.options);
        String execPart = "";
        if (jar != null) {
            if (!jar.exists()) {
                throw new IllegalStateException("Executable jar " + jar.getAbsolutePath() + " not found.");
            }
            command.add("-jar");
            command.add(jar.getPath());
            execPart = execPart + jar.getPath();
        }
        if (mainClassName != null) {
            command.add(mainClassName);
            execPart = execPart + " " + mainClassName;
        }

        command.addAll(Arrays.asList(arguments));
        JkLog.startln("Starting java program : " + execPart);
        JkLog.info(command, JkLog.verbose() ? -1 : 120);
        final int result;
        try {
            final Process process = processBuilder(command, optionAndEnv.env).start();

            final StreamGobbler outputStreamGobbler = JkUtilsIO.newStreamGobbler(
                    process.getInputStream(), JkLog.infoStream());
            final StreamGobbler errorStreamGobbler = JkUtilsIO.newStreamGobbler(
                    process.getErrorStream(), JkLog.warnStream());
            process.waitFor();
            outputStreamGobbler.stop();
            errorStreamGobbler.stop();
            result = process.exitValue();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        if (result != 0) {
            throw new IllegalStateException("Process terminated in error : exit value = " + result
                    + ".");
        }
        JkLog.done();
    }

    private OptionAndEnv optionsAndEnv() {
        final List<String> options = new LinkedList<>();
        final Map<String, String> env = new HashMap<>();
        if (classpath != null && !classpath.isEmpty()) {
            final String classpathString = classpath.toString();
            if (JkUtilsSystem.IS_WINDOWS && classpathString.length() > 7500) {
                JkLog.warn("classpath too long, classpath will be passed using CLASSPATH env variable.");
                env.put("CLASSPATH", classpathString);
            } else {
                options.add("-cp");
                options.add(classpath.toString());
            }
        }
        for (final AgentLibAndOption agentLibAndOption : agents) {
            final StringBuilder builder = new StringBuilder("-javaagent:")
                    .append(agentLibAndOption.lib);
            if (!JkUtilsString.isBlank(agentLibAndOption.options)) {
                builder.append("=").append(agentLibAndOption.options);
            }
            options.add(builder.toString());
        }
        for (final String key : this.sytemProperties.keySet()) {
            final String value = this.sytemProperties.get(key);
            options.add("-D" + key + "=" + value);
        }
        options.addAll(this.options);
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

    /**
     * Returns the classpth of this {@link JkJavaProcess}.
     * @return
     */
    public JkClasspath classpath() {
        return classpath;

    }

}
