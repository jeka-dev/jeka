package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    private String[] forkParams;

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

    public JkJavaCompilerToolChain setJavacProcessJavaHome(Path javaHome) {
        return this.setJavacProcess(JkProcess.of(javaHome.resolve("bin/javac").toString()));
    }

    /**
     * Invoking this method will make the compilation occurs in a forked process
     * if no compileTool has been specified.<br/>
     * The forked process will be a javac command taken from the running jdk or
     * an extra-one according the source version.
     */
    public JkJavaCompilerToolChain setForkedWithDefaultProcess(String... processParams) {
        this.forkParams = processParams;
        return this;
    }

    public JkJavaCompilerToolChain setJdkHints(JkJdks jdks, boolean preferTool) {
        JdkHints jdkHints = new JdkHints(jdks, preferTool);
        this.jdkHints = jdkHints;
        return this;
    }

    private ToolOrProcess guess(JkJavaVersion javaVersion) {
        if (javaVersion == null) {
            if (jdkHints.preferTool && ToolProvider.getSystemJavaCompiler() != null) {
                JkLog.trace("Use current JDK tool to compile.");
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

    public boolean isToolOrProcessSpecified() {
        return toolOrProcess.isSpecified();
    }

    private static JavaCompiler compileToolOrFail() {
        JavaCompiler result = ToolProvider.getSystemJavaCompiler();
        if (result == null) {
            throw new IllegalArgumentException("The current running Java platform does not provide a compiler. " +
                    "Please, run this program with a JDK and not a JRE");
        }
        return result;
    }

    /**
     * Actually compile the source files to the output directory. <br/>
     * This toolchain will try to find the most suitable JDK for performing
     * compilation.
     *
     * @param javaVersion Can be <code>null</code>. Provides the version of JDK
     *                    to use for <i>javac</i>. If <code>null</code>, it will try
     *                    to infer JDK version from #compileSpec.
     * @param compileSpec Contains options to pass to the Java compiler. It includes sources,
     *                    versions, and other options specified for the compiler.
     *
     * @return <code>false</code> if a compilation error occurred.
     *
     * @throws IllegalStateException if a compilation error occurred and the 'withFailOnError' flag is <code>true</code>.
     */
    public boolean compile(JkJavaVersion javaVersion, JkJavaCompileSpec compileSpec) {
        final Path outputDir = compileSpec.getOutputDir();
        List<String> options = compileSpec.getOptions();
        if (outputDir == null) {
            throw new IllegalArgumentException("Output dir option (-d) has not been specified on the compiler." +
                    " Specified options : " + printableOptions(options));
        }
        if (!compileSpec.getSources().andMatcher(JAVA_SOURCE_MATCHER).containFiles()) {
            JkLog.info("No source files found in " + compileSpec.getSources());
            return true;
        }
        JkUtilsPath.createDirectories(outputDir);
        String message = "Compile " + compileSpec.getSources()+ " to " + outputDir;
        if (JkLog.isVerbose()) {
            message = message + " using options : \n" + printableOptions(options);
        }
        JkLog.startTask(message);
        JkJavaVersion effectiveJavaVersion = Optional.ofNullable(javaVersion)
                .orElse(compileSpec.minJavaVersion());
        final boolean result = runCompiler(effectiveJavaVersion, compileSpec);
        JkLog.endTask();
        return result;
    }

    /**
     * @see #compile(JkJavaVersion, JkJavaCompileSpec)
     */
    public boolean compile(JkJavaCompileSpec compileSpec) {
        return compile(null, compileSpec);
    }

    private static String printableOptions(List<String> options) {
        StringBuilder sb = new StringBuilder();
        options.stream()
                .flatMap(item -> Stream.of(item.split(File.pathSeparator)))
                .forEach(item -> sb.append(item + "\n"));
        return sb.toString();
    }

    private boolean runCompiler(JkJavaVersion javaVersion, JkJavaCompileSpec compileSpec) {
        if (toolOrProcess.isSpecified()) {
            return toolOrProcess.run(compileSpec);
        }
        return guess(javaVersion).run(compileSpec);
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
        return task.call();
    }

    private static boolean runOnProcess(JkJavaCompileSpec compileSpec, Path javaHome) {
        return runOnProcess(compileSpec, JkProcess.of(javaHome + "/bin/javac"));
    }

    private static boolean runOnProcess(JkJavaCompileSpec compileSpec, JkProcess process) {
        JkLog.info("Compile using command " + process.getCommand());
        final List<String> sourcePaths = new LinkedList<>();
        List<Path> sourceFiles = compileSpec.getSources().andMatcher(JAVA_SOURCE_MATCHER).getFiles();
        sourceFiles.forEach(file -> sourcePaths.add(file.toString()));
        process.addParams(compileSpec.getOptions()).addParams(sourcePaths);
        JkLog.info(sourcePaths.size() + " files to compile.");
        final int result = process.exec();
        return (result == 0);
    }

    private static JavaCompiler getDefaultCompilerToolOrFail() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            String javaHome = System.getProperty("java.home");
            throw new IllegalStateException("The current Java Platform " + javaHome + " does not provide compiler."  +
                    "Try another JDK or specify property jeka.jdk.[version]=/path/to/jdk");
        }
        return compiler;
    }

    // Visible for testing
    static String runningJdkVersion() {
        final String fullVersion = System.getProperty("java.version");
        return runningJdkVersion(fullVersion);
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

    private String[] forkOptions() {
        return forkParams == null ? new String[0] : forkParams;
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

        ToolOrProcess(Path path) {
            this(JkProcess.of(path.resolve("bin/javac").toString()));
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
    }

}
