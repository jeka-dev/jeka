package dev.jeka.core.api.kotlin;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stand for a compilation setting and process. Use this class to perform java
 * compilation.
 */
public final class JkKotlinCompiler {

    enum Target {
        JAVA, JS
    }

    public static final String KOTLIN_VERSION_OPTION = "jeka.kotlin.version";

    public static final JkPathMatcher KOTLIN_SOURCE_MATCHER = JkPathMatcher.of("**/*.kt", "*.kt");

    private static final String KOTLIN_HOME = "KOTLIN_HOME";

    private boolean failOnError = true;

    private boolean logOutput;

    private boolean logCommand;

    private final List<String> jvmOptions = new LinkedList<>();

    private List<String> options = new LinkedList<>();

    private JkRepoSet repos = JkRepoSet.of(JkRepo.ofLocal(), JkRepo.ofMavenCentral());

    private List<Plugin> plugins = new LinkedList<>();

    private JkPathSequence extraClasspath = JkPathSequence.of();

    private final String command;

    private final JarsVersionAndTarget jarsVersionAndTarget;

    private String cachedVersion;  // for commandline compiler

    private JkKotlinCompiler(String command, JarsVersionAndTarget jarsVersionAndTarget) {
        super();
        this.command = command;
        this.jarsVersionAndTarget = jarsVersionAndTarget;
    }

    /**
     * Creates a {@link JkKotlinCompiler} based on the specified command. The specified command is supposed to be
     * accessible from the working directory. Examples of command are "kotlinc", "kotlinc-native", "/my/kotlin/home/kotlin-js", ...
     */
    public static JkKotlinCompiler ofCommand(String command) {
        String effectiveCommand = command;
        if (JkUtilsSystem.IS_WINDOWS && !command.toLowerCase().endsWith(".bat")) {
            effectiveCommand = command + ".bat";
        }
        return new JkKotlinCompiler(effectiveCommand, null);
    }

    /**
     * Creates a {@link JkKotlinCompiler} based on the specified command located in `KOTLIN_HOME` directory.
     * Examples of command are "kotlinc", "kotlin-jvm", "kotlin-js".
     */
    public static JkKotlinCompiler ofKotlinHomeCommand(String command) {
        String kotlinHome = System.getenv(KOTLIN_HOME);
        JkUtilsAssert.state(kotlinHome != null, KOTLIN_HOME
                + " environment variable is not defined. "
                + "Please define this environment variable in order to compile Kotlin sources.");
        String commandPath = kotlinHome + File.separator + "bin" + File.separator + command;
        return ofCommand(commandPath);
    }

    /**
     *  Creates a {@link JkKotlinCompiler} of the specified Kotlin version for the specified target platform. The
     *  compiler matching the specified Kotlin version is downloaded from the specified repo.
     */
    public static JkKotlinCompiler ofTarget(JkRepoSet repos, Target target, String kotlinVersion) {
        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repos)
                .resolve(JkDependencySet.of()
                        .and("org.jetbrains.kotlin:kotlin-compiler:" + kotlinVersion)
                )
                .assertNoError();
        return new JkKotlinCompiler(null,
                new JarsVersionAndTarget(resolveResult.getFiles(), kotlinVersion, target));
    }

    public static JkKotlinCompiler ofJvm(JkRepoSet repos, String version) {
        return ofTarget(repos, Target.JAVA, version);
    }

    public static JkKotlinCompiler ofJvm(JkRepoSet repos) {
        String version = JkProperties.get(KOTLIN_VERSION_OPTION);
        if (version == null) {
            JkLog.info("No jeka.kotlin.version specified, try to resolce Kotlin compiler on local machine");
            return ofKotlinHomeCommand("kotlinc");
        }
        JkLog.info("Kotlin JVM compiler resoled to version " + version);
        return ofJvm(repos, version);
    }

    /**
     * Returns true if this compiler is provided by the host machine, meaning it has not been downloaded and managed by Jeka.
     */
    public boolean isProvidedCompiler() {
        return command != null;
    }

    /**
     * Returns the version of Kotlin this compiler stands for. If this compiler is coming the hosting machine, this method
     * returns <code>null</code>.
     */
    public String getVersion() {
        if (jarsVersionAndTarget != null) {
            return jarsVersionAndTarget.version;
        }
        if (cachedVersion != null) {
            return cachedVersion;
        }
        List<String> lines = JkProcess.of(command, "-version").execAndReturnOutput();
        String line = lines.get(0);
        cachedVersion=  line.split(" ")[2].trim();
        return cachedVersion;
    }

    /**
     * Returns path of stdlib located in JEKA_HOME (if the compiler is provided by the platform) of from
     * a repo (if the comiler is managed by Jeka, meaning the version is specified)
     */
    public Path getStdLib() {
        if (isProvidedCompiler()) {
            String value = System.getenv("KOTLIN_HOME");
            JkUtilsAssert.state(value != null, KOTLIN_HOME + " environment variable is not defined.");
            return Paths.get(value).resolve("lib/kotlin-stdlib.jar");
        }
        return repos.get(JkCoordinate.of(JkKotlinModules.STDLIB));
    }

    public JkPathSequence getStdJdk8Lib() {
        if (isProvidedCompiler()) {
            String value = System.getenv("KOTLIN_HOME");
            JkUtilsAssert.state(value != null, KOTLIN_HOME + " environment variable is not defined.");
            Path kotlinDir = Paths.get(value);
            return JkPathSequence.of()
                    .and(kotlinDir.resolve("lib/kotlin-stdlib.jar"))
                    .and(kotlinDir.resolve("lib/kotlin-stdlib-jdk7.jar"))
                    .and(kotlinDir.resolve("lib/kotlin-stdlib-jdk8.jar"));
        }
        return JkDependencyResolver.of().addRepos(repos).resolve(JkDependencySet.of(JkKotlinModules.STDLIB_JDK8 + ":"
                        + getVersion())).getFiles();
    }

    public JkKotlinCompiler setFailOnError(boolean fail) {
        this.failOnError = fail;
        return this;
    }

    public JkKotlinCompiler setLogCommand(boolean log) {
        this.logCommand = log;
        return this;
    }

    public JkKotlinCompiler setLogOutput(boolean log) {
        this.logOutput = log;
        return this;
    }

    public JkRepoSet getRepos() {
        return repos;
    }

    /**
     * Set the repo to fetch stdlib and plugins
     */
    public JkKotlinCompiler setRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    /**
     * Adds JVM options to pass to compiler program (which is a Java program).
     */
    public JkKotlinCompiler addJvmOption(String option) {
        this.jvmOptions.add(toWindowsArg(option));
        return this;
    }

    public JkKotlinCompiler addPluginOption(String pluginId, String name, String value) {
        addOption("-P");
        addOption("plugin:" + pluginId + ":" + name + "=" + value);
        return this;
    }

    public JkKotlinCompiler addPlugin(Path pluginJar) {
        Plugin plugin = new Plugin();
        plugin.jar = pluginJar;
        plugins.add(plugin);
        return this;
    }

    public JkKotlinCompiler addPlugin(String pluginModule) {
        Plugin plugin = new Plugin();
        plugin.pluginCoordinate = pluginModule.isEmpty() ? null : JkCoordinate.of(pluginModule);
        plugins.add(plugin);
        return this;
    }

    public JkKotlinCompiler addOption(String option) {
        this.options.add(toWindowsArg(option));
        return this;
    }

    /**
     * Actually compile the source files to the output directory.
     *
     * @return <code>false</code> if a compilation error occurred.
     *
     * @throws IllegalStateException if a compilation error occurred and the 'withFailOnError' flag is <code>true</code>.
     */
    @SuppressWarnings("unchecked")
    public boolean compile(JkKotlinJvmCompileSpec compileSpec) {
        if (compileSpec.getSources().count(1, false) == 0) {
            JkLog.warn("No source to compile in " + compileSpec.getSources());
            return true;
        }
        final Path outputDir = compileSpec.getOutputDir();
        List<String> effectiveOptions = compileSpec.getOptions();
        effectiveOptions.addAll(0, this.options);
        if (outputDir == null) {
            throw new IllegalStateException("Output dir option (-d) has not been specified on the compiler. Specified options : " + effectiveOptions);
        }
        JkUtilsPath.createDirectories(outputDir);
        String message = "Compiling Kotlin sources " + compileSpec.getSources();
        if (JkLog.verbosity().isVerbose()) {
            message = message + " to " + outputDir + " using options : " + String.join(" ", effectiveOptions);
        }
        long start = System.nanoTime();
        JkLog.startTask(message);

        final Result result = run(compileSpec);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        if (!result.success) {
            if (failOnError) {
                throw new IllegalStateException("Kotlin compiler failed " + result.params);
            }
            return false;
        }
        return true;
    }

    public List<String> getPlugins() {
        return this.plugins.stream()
                .map(Plugin::toOption)
                .collect(Collectors.toList());
    }

    public List<String> getPluginOptions() {
        List<String> options = new LinkedList<>();
        for (Iterator<String> it = options.iterator(); it.hasNext();) {
            String option = it.next();
            if (option.equals("-P")) {
                if (it.hasNext()) {
                    options.add(option);
                    options.add(it.next());
                }
            }
        }
        return Collections.unmodifiableList(options);
    }

    private Result run(JkKotlinJvmCompileSpec compileSpec) {
        JkPathMatcher filter = KOTLIN_SOURCE_MATCHER.or(JkJavaCompiler.JAVA_SOURCE_MATCHER);
        final List<String> sourcePaths = new LinkedList<>();
        for (final Path file : compileSpec.getSources().andMatcher(filter).getFiles()) {
            sourcePaths.add(file.toString());

        }
        if (sourcePaths.isEmpty()) {
            JkLog.warn("No Kotlin source found in " + compileSpec.getSources());
            return new Result(true, Collections.emptyList());
        }
        JkLog.info("" + sourcePaths.size() + " files to compile.");
        JkLog.info("Kotlin version : " + getVersion() + ", Target JVM : " + compileSpec.getTargetVersion() );
        JkProcess kotlincProcess;
        List<String> loggedOptions = new LinkedList<>(this.options);
        JkKotlinJvmCompileSpec effectiveSpec = compileSpec.copy();
        for (Plugin plugin : this.plugins) {
            effectiveSpec.addOptions(plugin.toOption());
            loggedOptions.add(plugin.toOption());
        }
        if (command != null) {
            JkLog.info("Use kotlin compiler : " + command + " with options " + loggedOptions);
            kotlincProcess = JkProcess.of(command)
                    .addParams(this.jvmOptions.stream()
                            .map(JkKotlinCompiler::toJavaOption)
                            .collect(Collectors.toList()));
        } else {
            JkLog.info("Use kotlin compiler with options " + loggedOptions);
            kotlincProcess = JkJavaProcess.ofJava("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                    .setClasspath(jarsVersionAndTarget.jars)
                    .addJavaOptions(this.jvmOptions)
                    .addParams("-no-stdlib", "-no-reflect");
        }
        kotlincProcess
                    .addParams(toWindowsArgs(effectiveSpec.getOptions()))
                    .addParams(toWindowsArgs(options))
                    .addParams(toWindowsArgs(sourcePaths))
                    .setFailOnError(this.failOnError)
                    .setLogCommand(this.logCommand)
                    .setLogOutput(this.logOutput);
        final int result = kotlincProcess.exec();
        return new Result(result == 0, kotlincProcess.getParams());
    }

    private static class Result {
        final boolean success;
        final List<String> params;

        public Result(boolean success, List<String> params) {
            this.success = success;
            this.params = params;
        }
    }

    private static List<String> toWindowsArgs(List<String> args) {
        return args.stream().map(JkKotlinCompiler::toWindowsArg).collect(Collectors.toList());
    }

    private static String toWindowsArg(String arg) {
        if (!JkUtilsSystem.IS_WINDOWS) {
            return arg;
        }
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            return arg;
        }
        if (arg.contains(" ") || arg.contains(";") || arg.contains(",") || arg.contains("=")) {
            return '"' + arg + '"';
        }
        return arg;
    }

    private static class JarsVersionAndTarget {

        final JkPathSequence jars;

        final String version;

        final Target target;

        public JarsVersionAndTarget(JkPathSequence jars, String version, Target target) {
            JkUtilsAssert.argument(jars != null, "jars cannot be null");
            JkUtilsAssert.argument(version != null, "version cannot be null");
            JkUtilsAssert.argument(target != null, "target cannot be null");
            this.jars = jars;
            this.version = version;
            this.target = target;
        }
    }

    private static String toJavaOption(String option) {
        String result = option.startsWith("-") ? option.substring(1) : option;
        return "-J" + result;
    }

    private class Plugin {

        Path jar;

        JkCoordinate pluginCoordinate;

        private Path getJar() {
            if (jar != null) {
                return jar;
            }
            jar = repos.get(pluginCoordinate);
            if (jar == null) {
                throw new IllegalStateException("Cannot retrieve module " + pluginCoordinate);
            }
            return jar;
        }

        private String toOption() {
            return "-Xplugin=" + getJar();
        }

    }

}
