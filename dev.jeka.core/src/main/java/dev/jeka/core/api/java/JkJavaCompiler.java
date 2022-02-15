package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
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
public final class JkJavaCompiler<T> {

    /**
     * Filter to consider only Java source
     */
    public static final PathMatcher JAVA_SOURCE_MATCHER = JkPathMatcher.of(true, "**/*.java", "*.java");

    // Explicit compile command line
    private JkProcess compileProcess;

    // Explicit compile tool
    private JavaCompiler compileTool;

    private SortedMap<JkJavaVersion, Path> jdkHomes = Collections.emptySortedMap();

    private String[] forkParams;

    private String[] toolParams = new String[0];

    /**
     * Owner for parent chaining
     */
    public final T __;

    private JkJavaCompiler(T __) {
        this.__ = __;
    }

    /**
     * Creates a {@link JkJavaCompiler} without specifying a {@link JavaCompiler} instance or an external process.
     * When nothing is specified, this compiler will try the default {@link JavaCompiler} instance provided
     * by the running JDK.
     */
    public static JkJavaCompiler<Void> of() {
        return ofParent(null);
    }

    /**
     * Same as {@link #of()} but mentioning an owner for parent chaining.
     */
    public static <T> JkJavaCompiler<T> ofParent(T parent) {
        return new JkJavaCompiler(parent);
    }

    /**
     * Sets underlying java compiler tool to use.
     * Since in-process compilers cannot be run in forking process mode, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompiler<T> setCompileTool(JavaCompiler compiler, String... params) {
        this.compileTool = compiler;
        this.toolParams = params;
        this.compileProcess = null;
        return this;
    }

    /**
     * Sets the underlying compiler with the specified process. The process is typically a 'javac' command.
     */
    public JkJavaCompiler<T> setForkedWithProcess(JkProcess compileProcess) {
        this.compileTool = null;
        this.compileProcess = compileProcess;
        return this;
    }

    /**
     * Sets available JDK in order to choose the most appropriate version
     * for compiling.
     */
    public JkJavaCompiler<T> setJdkHomes(Map<JkJavaVersion, Path> jdks) {
        this.jdkHomes = new TreeMap<>(jdks);
        return this;
    }

    /**
     * Invoking this method will make the compilation occurs in a forked process
     * if no compileTool has been specified.<br/>
     * The forked process will be a javac command taken from the running jdk or
     * an extra-one according the source version.
     */
    public JkJavaCompiler<T> setForkedWithDefaultProcess(String... processParams) {
        this.compileTool = null;
        this.forkParams = processParams;
        return this;
    }

    /**
     * Sets available JDK in order to choose the most appropriate version
     * for compiling. Here the entries are expected to be formatted as jdk.12 => /path/to/jdk/home
     * @see #setJdkHomes(Map).
     */
    public JkJavaCompiler<T> setJdkHomesWithProperties(Map<String, String> jdkLocations) {
        TreeMap<JkJavaVersion, Path> jdks = new TreeMap<>();
        jdkLocations.entrySet().forEach( entry -> {
            final String version = JkUtilsString.substringAfterFirst(entry.getKey(), "jdk.");
            final String path = entry.getValue();
            if (version != null && path != null) {
                Path filePath = Paths.get(path);
                jdks.put(JkJavaVersion.of(version), filePath);
            }
        });
        this.jdkHomes = jdks;
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
    public boolean compile(JkJavaCompileSpec compileSpec) {
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
        final boolean result = runCompiler(compileSpec);
        JkLog.endTask();
        return result;
    }

    private static String printableOptions(List<String> options) {
        StringBuilder sb = new StringBuilder();
        options.stream()
                .flatMap(item -> Stream.of(item.split(";")))
                .forEach(item -> sb.append(item + "\n"));
        return sb.toString();
    }

    private static List<File> toFiles(Collection<Path> paths) {
        return JkUtilsPath.toFiles(paths);
    }

    private boolean runCompiler(JkJavaCompileSpec compileSpec) {
        JkJavaVersion runningJdkVersion = JkJavaVersion.of(runningJdkVersion());
        if (compileTool != null) {
            if (compileSpec.getSourceVersion() == null
                    || canCompile(compileTool, compileSpec.getSourceVersion())) {
                return runOnTool(compileSpec, compileTool, toolParams);
            } else {
                throw new IllegalStateException("Tool compiler does not support Java source version "
                        + compileSpec.getSourceVersion()
                        + ". It only supports " + compileTool.getSourceVersions());
            }
        }
        if (compileProcess != null) {
            runOnProcess(compileSpec, compileProcess);
        }

        // Try to use running JDK compiler
        JavaCompiler runningJdkCompilerTool = getDefaultOrFail();
        if (compileSpec.getSourceVersion() == null
                || compileSpec.getSourceVersion().equals(runningJdkVersion)) {
            if (forkParams == null) {
                return runOnTool(compileSpec, runningJdkCompilerTool, toolParams);
            } else {
                return runOnProcess(compileSpec, JkProcess.ofJavaTool("javac", forkOptions()));
            }
        }
        JkProcess process = resolveCompileProcessFromJdksForVersion(compileSpec.getSourceVersion());
        if (process != null) {
            return runOnProcess(compileSpec, process.addParams(forkOptions()));
        }
        if (canCompile(runningJdkCompilerTool, compileSpec.getSourceVersion())) {
            return runOnTool(compileSpec, runningJdkCompilerTool, toolParams);
        }
        throw new IllegalStateException("Cannot find suitable JDK to compile version "
                + compileSpec.getSourceVersion()
                + "\nRunning JDK is " + runningJdkVersion + " and known JDK homes are " + this.jdkHomes);
    }

    private static boolean runOnTool(JkJavaCompileSpec compileSpec, JavaCompiler compiler, String[] toolOptions) {
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        List<File> files = JkUtilsPath.toFiles(compileSpec.getSources().withMatcher(JAVA_SOURCE_MATCHER).getFiles());
        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
        List<String> options = new LinkedList<>();
        options.addAll(Arrays.asList(toolOptions));
        options.addAll(compileSpec.getOptions());
        CompilationTask task = compiler.getTask(new PrintWriter(JkLog.getOutPrintStream()),
                null, new JkDiagnosticListener(), options, null, javaFileObjects);
        return task.call();
    }

    private static boolean runOnProcess(JkJavaCompileSpec compileSpec, JkProcess process) {
        JkLog.info("Compile using command " + process.getCommand());
        Path workingDir = Optional.ofNullable(process.getWorkingDir()).orElse(Paths.get(""));
        final List<String> sourcePaths = new LinkedList<>();
        List<Path> sourceFiles = compileSpec.getSources().andMatcher(JAVA_SOURCE_MATCHER).getFiles();
        sourceFiles.forEach(file -> sourcePaths.add(file.toString()));
        process.addParams(compileSpec.getOptions()).addParams(sourcePaths);
        JkLog.info("" + sourcePaths.size() + " files to compile.");
        final int result = process.exec();
        return (result == 0);
    }

    private static JavaCompiler getDefaultOrFail() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JkUtilsAssert.state(compiler != null, "This platform does not provide compileRunner. " +
                "Try another JDK or specify option jdk.[version]=/path/to/jdk");
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



    private static boolean canCompile(JavaCompiler compilerTool, JkJavaVersion version) {
        return compilerTool.getSourceVersions().stream()
                .map(sourceVersion -> JkUtilsString.substringAfterFirst(sourceVersion.name(), "RELEASE_"))
                .map(JkJavaVersion::of)
                .findAny().isPresent();
    }

    private String[] forkOptions() {
        return forkParams == null ? new String[0] : forkParams;
    }

    /**
     * Returns a suitable javac from jdk homes to compile with specified source version.
     */
    private JkProcess resolveCompileProcessFromJdksForVersion(JkJavaVersion sourceVersion) {
        Path jdkPath = null;
        for (Map.Entry<JkJavaVersion, Path> entry : jdkHomes.entrySet()) {
            if (entry.getKey().compareTo(sourceVersion) < 0) {
                continue;
            }
            Path path = entry.getValue();
            if (!Files.exists(path)) {
                JkLog.warn("JdkHome " + entry.getKey() + " " + path + " does not exist.");
                continue;
            }
            JkLog.info("Found Jdk " + entry.getKey() + " " + path + " to compile for with source version="
                    + sourceVersion);
            jdkPath = path;
            break;
        }
        if (jdkPath == null) {
            return null;
        }
        final String cmd = jdkPath + "/bin/javac";
        return JkProcess.of(cmd);
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

}
