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

package dev.jeka.core.api.kotlin;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Kotlin compiler.
 */
public final class JkKotlinCompiler {

    /**
     * Represents the possible target platforms for the Kotlin compiler.
     */
    public enum Target {
        JAVA, JS
    }

    public static final String KOTLIN_COMPILER_COORDINATES = "org.jetbrains.kotlin:kotlin-compiler:";

    public static final String KOTLIN_VERSION_OPTION = "jeka.kotlin.version";

    public static final JkPathMatcher KOTLIN_SOURCE_MATCHER = JkPathMatcher.of("**/*.kt", "*.kt");

    private static final String KOTLIN_HOME = "KOTLIN_HOME";

    public static final String ALLOPEN_PLUGIN_COORDINATES = "org.jetbrains.kotlin:kotlin-allopen-compiler-plugin";

    public static final String ALLOPEN_PLUGIN_ID = "org.jetbrains.kotlin.allopen";

    public static final String NOARG_PLUGIN_ID = "org.jetbrains.kotlin.noarg";

    public static final String SERIALIZATION_PLUGIN_ID = "org.jetbrains.kotlin.plugin.serialization";

    private boolean failOnError = true;

    private boolean logOutput = JkLog.isVerbose();

    private boolean logCommand = JkLog.isVerbose();

    private final List<String> jvmOptions = new LinkedList<>();

    private final List<String> options = new LinkedList<>();

    private JkRepoSet repos = JkRepoSet.of(JkRepo.ofMavenCentral());

    private final List<Plugin> plugins = new LinkedList<>();

    private final JkPathSequence extraClasspath = JkPathSequence.of();

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
    public static JkKotlinCompiler ofTarget(
            JkRepoSet repos,
            Target target,
            @JkDepSuggest(versionOnly = true, hint = KOTLIN_COMPILER_COORDINATES) String kotlinVersion) {

        JkPathTree kotlincDir = JkPathTree.of(getLibsDir(kotlinVersion));
        JkPathSequence kotlincFiles;
        if (kotlincDir.exists() && kotlincDir.containFiles()) {
           kotlincFiles = JkPathSequence.of(kotlincDir.getFiles());
        } else {
            JkLog.startTask("Downloading Kotlin compiler " + kotlinVersion);
            kotlincFiles= JkDependencyResolver.of(repos)
                    .resolve(JkDependencySet.of()
                            .and("org.jetbrains.kotlin:kotlin-compiler:" + kotlinVersion)
                    )
                    .assertNoError()
                    .getFiles();
            JkLog.endTask();
            kotlincDir.importFiles(kotlincFiles.getEntries());
        }
        return new JkKotlinCompiler(null,
                new JarsVersionAndTarget(kotlincFiles, kotlinVersion, target));
    }

    /**
     * Creates a {@link JkKotlinCompiler} for JVM with the specified version and fetched from the specified repository .
     *
     * @param repos The repository set to download the Kotlin compiler from.
     * @param version The version of Kotlin to use.
     *
     * @throws IllegalArgumentException if `version` is null.
     */
    public static JkKotlinCompiler ofJvm(
            JkRepoSet repos,
            @JkDepSuggest(versionOnly = true, hint = KOTLIN_COMPILER_COORDINATES) String version) {

        JkUtilsAssert.argument(version != null, "Kotlin version cannot be null. You mist provide one.");
        return ofTarget(repos, Target.JAVA, version);
    }

    /**
     * Creates a {@link JkKotlinCompiler} for JVM with the specified by 'jeka.kotlin.version' property
     * and fetched from the specified repository.
     *
     * @param repos The repository set to download the Kotlin compiler from.
     */
    public static JkKotlinCompiler ofJvm(JkRepoSet repos) {
        JkProperties props = JkProperties.ofStandardProperties();
        String version = props.get(KOTLIN_VERSION_OPTION);
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
        List<String> lines = JkProcess.of(command, "-version").setCollectStdout(true).exec().getStdoutAsMultiline();
        String line = lines.get(0);
        cachedVersion=  line.split(" ")[2].trim();
        return cachedVersion;
    }

    /**
     * Returns path of stdlib located in JEKA_HOME (if the compiler is provided by the platform) of from
     * a repo (if the compiler is managed by JeKa, meaning the version is specified)
     */
    public Path getStdLib() {
        if (isProvidedCompiler()) {
            String value = System.getenv("KOTLIN_HOME");
            JkUtilsAssert.state(value != null, KOTLIN_HOME + " environment variable is not defined.");
            return Paths.get(value).resolve("lib/kotlin-stdlib.jar");
        }
        JkCoordinate coordinate = JkCoordinate.of(JkKotlinModules.STDLIB).withVersion(this.getVersion());
        return JkCoordinateFileProxy.of(repos, coordinate).get();
    }

    /**
     * Returns the path of the standard JDK 8 libraries for the Kotlin compiler.
     */
    public JkPathSequence getStdJdk8Lib() {
        final Path jarDir;
        if (isProvidedCompiler()) {
            String value = System.getenv("KOTLIN_HOME");
            JkUtilsAssert.state(value != null, KOTLIN_HOME + " environment variable is not defined.");
            jarDir = Paths.get(value).resolve("lib");
            return JkPathSequence.of()
                    .and(jarDir.resolve("kotlin-stdlib.jar"))
                    .and(jarDir.resolve("kotlin-stdlib-jdk7.jar"))
                    .and(jarDir.resolve("kotlin-stdlib-jdk8.jar"));
        } else {
            jarDir = getLibsDir(getVersion());
            return JkPathSequence.of()
                    .and(jarDir.resolve(String.format("kotlin-stdlib-%s.jar", getVersion())))
                    .and(jarDir.resolve(String.format("kotlin-stdlib-jdk7-%s.jar", getVersion())))
                    .and(jarDir.resolve(String.format("kotlin-stdlib-jdk8-%s.jar", getVersion())));
        }

    }

    /**
     * Sets the flag to indicate whether compilation should fail on error or not.
     */
    public JkKotlinCompiler setFailOnError(boolean fail) {
        this.failOnError = fail;
        return this;
    }

    /**
     * Sets the flag to indicate whether the compiler command should be logged or not.
     */
    public JkKotlinCompiler setLogCommand(boolean log) {
        this.logCommand = log;
        return this;
    }

    /**
     * Sets the flag to indicate whether the compiler output should be logged or not.
     */
    public JkKotlinCompiler setLogOutput(boolean log) {
        this.logOutput = log;
        return this;
    }

    /**
     * Retrieves the set of repositories to fetch stdlib and plugins.
     */
    public JkRepoSet getRepos() {
        return repos;
    }

    /**
     * Set the repo to fetch stdlib and plugins.
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

    /**
     * Adds a plugin option to the Kotlin compiler.
     */
    public JkKotlinCompiler addPluginOption(String pluginId, String name, String value) {
        this.options.addAll(toPluginOption(pluginId, name, value));
        return this;
    }

    List<String> toPluginOption(String pluginId, String name, String value) {
        List<String> result = new ArrayList<>();
        result.add("-P");
        result.add("plugin:" + pluginId + ":" + name + "=" + value);
        return result;
    }

    /**
     * Adds a plugin JAR file to the Kotlin compiler.
     */
    public JkKotlinCompiler addPlugin(Path pluginJar) {
        Plugin plugin = new Plugin();
        plugin.jar = pluginJar;
        plugins.add(plugin);
        return this;
    }

    /**
     * Instructs this compiler to use the plugin identified by the specified coordinates.
     * If the coordinate does not mention the version, the Kotlin version of this compiler is chosen.<p>
     * {@link Plugin} class provides constants about most common plugin coordinates.
     */
    public JkKotlinCompiler addPlugin(@JkDepSuggest(hint = "org.jetbrains.kotlin:") String coordinate) {
        Plugin plugin = new Plugin();
        if (JkUtilsString.isBlank(coordinate)) {
            plugin.pluginCoordinate = null;
        } else {
            JkCoordinate effectiveCoordinate = JkCoordinate.of(coordinate);
            if (effectiveCoordinate.getVersion().isUnspecified()) {
                effectiveCoordinate = effectiveCoordinate.withVersion(getVersion());
            }
            plugin.pluginCoordinate = effectiveCoordinate;
        }
        plugins.add(plugin);
        return this;
    }

    /**
     * Adds an option to the Kotlin compiler.
     */
    public JkKotlinCompiler addOption(String option) {
        this.options.add(toWindowsArg(option));
        return this;
    }

    /**
     * Compiles the source files to the output directory.
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

        JkLog.verboseStartTask("compile-kotlin-sources");
        JkLog.verbose("Compiling Kotlin sources " + compileSpec.getSources());
        if (JkLog.isVerbose()) {
            JkLog.verbose("Options: " + JkUtilsString.formatOptions(compileSpec.getOptions()));

        }
        final Result result = run(compileSpec);
        JkLog.verboseEndTask();
        if (!result.success) {
            if (failOnError) {
                throw new IllegalStateException("Kotlin compiler failed " + result.params);
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the list of plugins used by this compiler.
     */
    public List<String> getPlugins() {
        return this.plugins.stream()
                .map(Plugin::toOption)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of plugin options used by this compiler.
     */
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

    public List<String> getOptions() {
        return Collections.unmodifiableList(this.options);
    }

    private static Path getLibsDir(
            @JkDepSuggest(versionOnly = true, hint = KOTLIN_COMPILER_COORDINATES) String version) {

        return JkLocator.getCacheDir().resolve("kotlinc").resolve(version);
    }

    private Result run(JkKotlinJvmCompileSpec compileSpec) {
        JkPathMatcher filter = KOTLIN_SOURCE_MATCHER.or(JkJavaCompilerToolChain.JAVA_SOURCE_MATCHER);
        final List<String> sourcePaths = new LinkedList<>();
        for (final Path file : compileSpec.getSources().andMatcher(filter).getFiles()) {
            sourcePaths.add(file.toString());

        }
        if (sourcePaths.isEmpty()) {
            JkLog.warn("No Kotlin source found in " + compileSpec.getSources());
            return new Result(true, Collections.emptyList());
        }
        JkLog.verbose("%s files to compile.", sourcePaths.size());
        JkLog.verbose("Kotlin version : %s, Target JVM : %s", getVersion() ,compileSpec.getTargetVersion() );
        JkAbstractProcess<?> kotlincProcess;
        List<String> loggedOptions = new LinkedList<>(this.options);
        JkKotlinJvmCompileSpec effectiveSpec = compileSpec.copy();
        for (Plugin plugin : this.plugins) {
            effectiveSpec.addOptions(plugin.toOption());
            loggedOptions.add(plugin.toOption());
        }
        if (command != null) {
            JkLog.verbose("Use kotlinc compiler : %s with options %s", command, JkUtilsString.formatOptions(loggedOptions));
            kotlincProcess = JkProcess.of(command)
                    .addParams(this.jvmOptions.stream()
                            .map(JkKotlinCompiler::toJavaOption)
                            .collect(Collectors.toList()));
        } else {
            JkPathSequence kotlincClasspath = jarsVersionAndTarget.jars;
            JkLog.verbose("Use Java-Kotlin compiler using jars: %n%s", kotlincClasspath.toPathMultiLine("  "));
            JkLog.verbose("Use Kotlin compiler with options %s", JkUtilsString.formatOptions(loggedOptions));
            kotlincProcess = JkJavaProcess.ofJava( "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                    .setClasspath(kotlincClasspath)
                    .addJavaOptions(this.jvmOptions)
                    .addParams("-no-stdlib", "-no-reflect");
        }
        kotlincProcess
                    .addParams(toWindowsArgs(effectiveSpec.getOptions()))
                    .addParams(toWindowsArgs(options))
                    .addParams(toWindowsArgs(sourcePaths))
                    .setFailOnError(this.failOnError)
                    .setLogCommand(this.logCommand)
                    .setCollectStdout(!this.logOutput)
                    .setCollectStderr(!this.logOutput)
                    .setLogWithJekaDecorator(this.logOutput);
        final JkProcResult result = kotlincProcess.exec();
        return new Result(result.hasSucceed(), kotlincProcess.getParams());
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

        @Override
        public String toString() {
            return "JarsVersionAndTarget{" +
                    "jars=" + jars +
                    ", version='" + version + '\'' +
                    ", target=" + target +
                    '}';
        }

        private String formattedToString() {
            StringBuilder sb = new StringBuilder();
            sb.append("version=").append(version).append("\n");
            sb.append("target=").append(target).append("\n");
            sb.append("jars").append("\n");
            jars.forEach(jar -> {sb.append("  ").append(jar).append("\n");});
            return sb.toString();
        }
    }

    private static String toJavaOption(String option) {
        String result = option.startsWith("-") ? option.substring(1) : option;
        return "-J" + result;
    }

    private List<Path> pluginsCp() {
        return plugins.stream()
                .map(Plugin::getJar)
                .collect(Collectors.toList());
    }

    private class Plugin {

        Path jar;

        JkCoordinate pluginCoordinate;

        private Path getJar() {
            if (jar != null) {
                return jar;
            }
            return JkCoordinateFileProxy.of(repos, pluginCoordinate).get();
        }

        private String toOption() {
            return "-Xplugin=" + getJar();
        }

    }

}
