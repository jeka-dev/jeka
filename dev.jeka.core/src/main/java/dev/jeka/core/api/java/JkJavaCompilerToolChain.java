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

package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;

/**
 * Compiler for Java source code. Underlying, it uses either a {@link JavaCompiler} instance either an external
 * process (forked mode).
 * The compiler selection follows these rules :
 * <li>
 *     <ul>If a compiler tool is specified then compilation will use it</ul>
 *     <ul>If a compiler process is specified then compilation will use it</ul>
 *     <ul>If the Java source version is same than running JDK then compilation will use it</ul>
 *     <ul>If the Java source version is different than running JDK then compilation will use best fitted compiler
 *         among the ones specified in jdkHomes</ul>
 * </li>
 */
public final class JkJavaCompilerToolChain {

    /**
     * Filter to consider only Java source
     */
    public static final PathMatcher JAVA_SOURCE_MATCHER = JkPathMatcher.of(true, "**/*.java", "*.java");

    private ToolOrProcess toolOrProcess = new ToolOrProcess(null, null);

    private String[] toolParams = new String[0];

    private JdkHints jdkHints = JdkHints.ofDefault();

    private JkJavaCompilerToolChain() {
    }

    /**
     * Creates a {@link JkJavaCompilerToolChain} without specifying a {@link JavaCompiler} instance or an external process.
     * When nothing is specified, this compiler will try the default {@link JavaCompiler} instance provided
     * by the running JDK.
     */
    public static JkJavaCompilerToolChain of() {
        return new JkJavaCompilerToolChain();
    }

    /**
     * Sets underlying java compiler tool to use.
     * Since in-process compilers cannot be run in forking process mode, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompilerToolChain setCompileTool(JavaCompiler tool, String... params) {
        this.toolOrProcess = new ToolOrProcess(tool, null);
        this.toolParams = params;
        return this;
    }

    /**
     * Sets the underlying compiler with the specified process. The process is typically a 'javac' command.
     */
    public JkJavaCompilerToolChain setJavacProcess(JkProcess compileProcess) {
        this.toolOrProcess = new ToolOrProcess(null, compileProcess);
        return this;
    }

    /**
     * Sets the JDK hints to specify the available JDKs and whether to prefer in-process tool for compilation.
     *
     * @param jdks A {@link JkJdks} instance defining the JDKs to be considered during compilation.
     * @param preferInProcess If true, the compiler tool is preferred over process-based compilation;
     *                   otherwise, the process is preferred.
     * @return The current instance of {@link JkJavaCompilerToolChain} with updated JDK hints.
     */
    public JkJavaCompilerToolChain setJdkHints(JkJdks jdks, boolean preferInProcess) {
        JdkHints jdkHints = new JdkHints(jdks, preferInProcess);
        this.jdkHints = jdkHints;
        return this;
    }

    /**
     * Configures whether the Java compilation process should be forked into
     * an external process.
     *
     * @param forkCompiler If true, the compilation process will be forked into
     *                     an external process. If false, the compilation will
     *                     occur in-process.
     * @return The current instance of {@link JkJavaCompilerToolChain} with the
     *         updated fork configuration.
     */
    public JkJavaCompilerToolChain setForkCompiler(boolean forkCompiler) {
        this.jdkHints = new JdkHints(this.jdkHints.jdks, !forkCompiler);
        return this;
    }

    public boolean isToolOrProcessSpecified() {
        return toolOrProcess.isSpecified();
    }

    /**
     * Actually compile the source files to the output directory. <br/>
     * This toolchain will try to find the most suitable JDK for performing
     * compilation.
     *
     * @param targetVersion Can be <code>null</code>. Provides the version of JDK
     *                    to use for <i>javac</i>. If <code>null</code>, it will try
     *                    to infer JDK version from #compileSpec.
     * @param compileSpec Contains options to pass to the Java compiler. It includes sources,
     *                    versions, and other options specified for the compiler.
     *
     * @return <code>false</code> if a compilation error occurred.
     *
     * @throws IllegalStateException if a compilation error occurred and the 'withFailOnError' flag is <code>true</code>.
     */
    public boolean compile(JkJavaVersion targetVersion, JkJavaCompileSpec compileSpec) {
        final Path outputDir = compileSpec.getOutputDir();
        List<String> options = compileSpec.getOptions();
        if (outputDir == null) {
            throw new IllegalArgumentException("Output dir option (-d) has not been specified on the compiler." +
                    " Specified options : " + JkUtilsString.readableCommandAgs("    ", options));
        }
        if (!compileSpec.getSources().andMatcher(JAVA_SOURCE_MATCHER).containFiles()) {
            JkLog.warn("No Java source files found in %s", compileSpec.getSources());
            return true;
        }
        JkUtilsPath.createDirectories(outputDir);

        if (JkLog.isVerbose()) {
            JkLog.startTask("[VERBOSE] compile");
            JkLog.verbose("sources      : " + compileSpec.getSources());
            JkLog.verbose("class dir    : " + compileSpec.getOutputDir());
            JkLog.verbose("source count : " + compileSpec.getSources().count(Integer.MAX_VALUE, false));
        }
        if (JkLog.isDebug()) {
            JkLog.debug("with options : " );
            JkLog.debug(JkUtilsString.readableCommandAgs("    ", compileSpec.getOptions()));
        }
        JkJavaVersion effectiveJavaVersion = Optional.ofNullable(targetVersion)
                .orElse(compileSpec.minJavaVersion());
        final boolean result = runCompiler(effectiveJavaVersion, compileSpec);
        if (JkLog.isVerbose()) {
            JkLog.endTask("Compilation " + (result ? "completed successfully" : "failed"));
        }
        return result;
    }

    /**
     * @see #compile(JkJavaVersion, JkJavaCompileSpec)
     */
    public boolean compile(JkJavaCompileSpec compileSpec) {
        return compile(null, compileSpec);
    }

    /**
     * Determines if the Java compilation process is forked into an external process.
     * @return true if the compilation process is forked into an external process, false otherwise.
     */
    public boolean isCompilationForked(JkJavaVersion javaVersion, JkJavaCompileSpec compileSpec) {
        JkJavaVersion effectiveJavaVersion = Optional.ofNullable(javaVersion)
                .orElse(compileSpec.minJavaVersion());
        ToolOrProcess toolOrProcess = guess(effectiveJavaVersion);
        return toolOrProcess.isForkedInProcess();
    }

    // Visible for testing
    static String runningJdkVersion(String fullVersion) {
        String[] items = fullVersion.split("\\.");
        if (items.length == 1 ) {
            return fullVersion;
        }
        if ("1".equals(items[0])) {
            return items[1];
        }
        return items[0];
    }

    private static JavaCompiler compileToolOrFail() {
        JavaCompiler result = ToolProvider.getSystemJavaCompiler();
        if (result == null) {
            throw new IllegalArgumentException("The current running Java platform does not provide a compiler. " +
                    "Please, run this program with a JDK and not a JRE");
        }
        return result;
    }

    private boolean runCompiler(JkJavaVersion javaVersion, JkJavaCompileSpec compileSpec) {
        if (toolOrProcess.isSpecified()) {
            return toolOrProcess.run(compileSpec);
        }
        return guess(javaVersion).run(compileSpec);
    }

    private ToolOrProcess guess(JkJavaVersion javaVersion) {
        if (javaVersion == null) {
            if (jdkHints.preferTool && ToolProvider.getSystemJavaCompiler() != null) {
                JkLog.verbose("Use current JDK tool to compile.");
                return new ToolOrProcess(compileToolOrFail());
            }
            Path javaHome = Paths.get(System.getProperty("java.home"));
            if (!Files.exists(javaHome.resolve("bin/javac"))) {
                throw  new IllegalStateException("The current Java is not a JDK." +
                        " Please run a JDK or precise the java version to run.");
            }
            return new ToolOrProcess(javaHome);
        }
        Path currentJavaHome = Paths.get(System.getProperty("java.home"));
        boolean hasJavac = Files.exists(currentJavaHome.resolve("bin/java"));

        // It may be running on the jre home
        if (!hasJavac) {
            hasJavac = Files.exists(currentJavaHome.resolve("../bin/java"));
        }

        boolean currentVersionMatch = javaVersion.equals(JkJavaVersion.ofCurrent());
        if (currentVersionMatch) {
            if (jdkHints.preferTool || !hasJavac) {
                return new ToolOrProcess(compileToolOrFail());
            }
            return new ToolOrProcess(currentJavaHome);
        }
        Path specificJavaHome = jdkHints.jdks.getHome(javaVersion);
        if (specificJavaHome != null) {
            JkLog.info("Use JDK %s to compile for JVM %s", specificJavaHome, javaVersion);
            return new ToolOrProcess(specificJavaHome);
        }
        JkLog.warn("No JDK path defined for version %s. Will use embedded compiler %s",
                javaVersion, JkJavaVersion.ofCurrent());
        return new ToolOrProcess(compileToolOrFail());
    }

    private static boolean runOnTool(JkJavaCompileSpec compileSpec, JavaCompiler compiler, String[] toolOptions) {
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        List<File> files = JkUtilsPath.toFiles(compileSpec.getSources().andMatcher(JAVA_SOURCE_MATCHER).getFiles());
        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
        List<String> options = new LinkedList<>();
        options.addAll(Arrays.asList(toolOptions));
        options.addAll(compileSpec.getOptions());
        CompilationTask task = compiler.getTask(new PrintWriter(JkLog.getOutPrintStream()),
                null, new JkDiagnosticListener(), options, null, javaFileObjects);
        if (JkLog.isVerbose()) {
            JkLog.verbose("Compile in-process.");
            if (JkLog.isDebug()) {
                JkLog.debug("Compile options: %s", options);
            } else {
                JkLog.verbose("Compile options: %s", JkUtilsString.ellipse(options.toString(), 100));
            }
        }
        return task.call();
    }

    private static boolean runOnProcess(JkJavaCompileSpec compileSpec, JkProcess process) {
        JkLog.info("Fork compile using command " + process.getParamAt(0));
        JkLog.info("Compile options: " + compileSpec.getOptions());
        final List<String> sourcePaths = new LinkedList<>();
        List<Path> sourceFiles = compileSpec.getSources().andMatcher(JAVA_SOURCE_MATCHER).getFiles();
        sourceFiles.forEach(file -> sourcePaths.add(file.toString()));
        process.addParams(compileSpec.getOptions()).addParams(sourcePaths);
        JkLog.info(sourcePaths.size() + " files to compile.");
        return process.exec().hasSucceed();
    }

    @SuppressWarnings("rawtypes")
    private static class JkDiagnosticListener implements DiagnosticListener {

        @Override
        public void report(Diagnostic diagnostic) {
            if (!diagnostic.getKind().equals(Diagnostic.Kind.ERROR)) {
                JkLog.info(diagnostic.toString());
            } else {
                System.out.println(diagnostic);
            }
        }
    }

    private static class JdkHints {
        final JkJdks jdks;
        final boolean preferTool;

        JdkHints(JkJdks jdks, boolean preferTool) {
            this.jdks = jdks;
            this.preferTool = preferTool;
        }

        static JdkHints ofDefault() {
            return new JdkHints(JkJdks.of(), true);
        }
    }

    private class ToolOrProcess {

        final JkProcess compileProcess;

        final JavaCompiler compileTool;

        ToolOrProcess(JavaCompiler compileTool, JkProcess compileProcess) {
            this.compileProcess = compileProcess;
            this.compileTool = compileTool;
        }

        ToolOrProcess(JavaCompiler compileTool) {
            this(compileTool, null);
        }

        ToolOrProcess(JkProcess compileProcess) {
            this(null, compileProcess);
        }

        ToolOrProcess(Path javaHome) {
            this(JkProcess.of(findJavac(javaHome).toString()));
        }

        boolean isSpecified() {
            return compileTool != null || compileProcess != null;
        }

        boolean run(JkJavaCompileSpec compileSpec) {
             if (compileTool != null) {
                 return runOnTool(compileSpec, compileTool, toolParams);
             } else if (compileProcess != null) {
                 return runOnProcess(compileSpec, compileProcess);
             } else {
                 throw new IllegalStateException("Neither compilation tool or process has been specified.");
             }
        }

        boolean isForkedInProcess() {
            return compileTool == null;
        }

    }

    private static Path findJavac(Path javaHome) {
        Path javac = javaHome.resolve("bin/javac");
        if (!Files.exists(javac)) {
            javac = javaHome.getParent().resolve("bin/javac");
        }
        return javac;
    }

    private static void loadOptionsIfNeeded(JkJavaCompileSpec compileSpec) {
        if (JkLog.isVerbose()) {
            JkLog.startTask("compile");
            JkLog.info("sources      : " + compileSpec.getSources());
            JkLog.info("to           : " + compileSpec.getOutputDir());
            JkLog.info("with options : " );
            JkLog.info(JkUtilsString.readableCommandAgs("    ", compileSpec.getOptions()));
        }
    }

    public static class JkJdks {

        private final Map<JkJavaVersion, Path> explicitJdkHomes;

        private JkJdks(Map<JkJavaVersion, Path> explicitJdkHomes) {
            this.explicitJdkHomes = explicitJdkHomes;
        }

        public static JkJdks of() {
            return new JkJdks(Collections.emptyMap());
        }

        public static JkJdks ofJdkHomeProps(Map<String, String> homes) {
            Map<JkJavaVersion, Path> map = new HashMap<>();
            for (Map.Entry<String, String> entry : homes.entrySet()) {
                map.put(JkJavaVersion.of(entry.getKey().trim()), Paths.get(entry.getValue().trim()));
            }
            return new JkJdks(map);
        }

        public Path getHome(JkJavaVersion javaVersion) {
            Path result = explicitJdkHomes.get(javaVersion);
            if (result == null && javaVersion.equals(JkJavaVersion.ofCurrent())) {
                return Paths.get(System.getProperty("java.home"));
            }
            if (result != null && !Files.exists(result)) {
                JkLog.warn("Specified path for JDK %s does not exists", result);
                return null;
            }
            return result;
        }
    }
}
