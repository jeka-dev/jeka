package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
public final class JkJavaCompiler<T> {

    // Explicit compile command line
    private JkProcess compileProcess;

    // Explicit compile tool
    private JavaCompiler compileTool;

    private SortedMap<JkJavaVersion, Path> jdkHomes = Collections.emptySortedMap();

    private String[] forkOptions;

    private String[] toolOptions = new String[0];

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
     * Same as {@link #of()} but mentioning a owner for parent chaining.
     */
    public static <T> JkJavaCompiler<T> ofParent(T parent) {
        return new JkJavaCompiler(parent);
    }

    /**
     * Sets underlying java compiler tool to use.
     * Since in-process compilers cannot be run in forking process mode, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompiler<T> setCompileTool(JavaCompiler compiler, String... options) {
        this.compileTool = compiler;
        this.toolOptions = options;
        this.compileProcess = null;
        return this;
    }

    /**
     * Sets the underlying compiler with the specified process. The process is typically a 'javac' command.
     */
    public JkJavaCompiler<T> setCompileProcess(JkProcess compileProcess) {
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
    public JkJavaCompiler<T> setForkParams(String... options) {
        this.forkOptions = options;
        return this;
    }

    /**
     * Sets available JDK in order to choose the most appropriate version
     * for compiling. Here the entries are expected to be formatted as jdk.12 => /path/to/jdk/home
     * @see #setJdkHomes(Map).
     */
    public JkJavaCompiler<T> setJdkHomeProps(Map<String, String> jdkLocations) {
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
     * Sets to default underlying compiler, meaning the compiler tool embedded in the running JDK
     */
    public JkJavaCompiler<T> setDefault() {
        this.compileProcess = null;
        this.compileTool = null;
        return this;
    }

    /**
     * Returns <code>true</code> if no compiler or fork has been set on.
     */
    public boolean isDefault() {
        return compileTool == null && compileProcess == null;
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
            throw new IllegalArgumentException("Output dir option (-d) has not been specified on the compiler. Specified options : " + options);
        }
        if (compileSpec.getSourceFiles().isEmpty()) {
            JkLog.info("No sources file or directory specified.");
            return true;
        }
        List<File> files = toFiles(compileSpec.getSourceFiles());
        if (files.isEmpty()) {
            JkLog.info("No Java source files found.");
            return true;
        }
        JkUtilsPath.createDirectories(outputDir);
        String message = "Compile " + compileWhatMessage(compileSpec.getSourceFiles()) + " to " + outputDir;
        if (JkLog.verbosity().isVerbose()) {
            message = message + " using options : " + String.join(" ", options);
        }
        JkLog.startTask(message);
        final boolean result = runCompiler(compileSpec);
        JkLog.endTask();
        return result;
    }

    private static String compileWhatMessage(List<Path> paths) {
        List<String> folders = new LinkedList<>();
        List<String> files = new LinkedList<>();
        for (Path path : paths) {
            Path relPath = JkUtilsPath.relativizeFromWorkingDir(path);
            if (Files.isDirectory(path)) {
                folders.add(relPath.toString());
            } else {
                files.add(relPath.toString());
            }
        }
        if (paths.size() < 4) {
            List<String> all = JkUtilsIterable.concatLists(folders, files);
            return String.join(", ", all);
        }
        else {
            return JkUtilsString.plurialize(folders.size(), "folder") + " and " +
                    JkUtilsString.plurialize(files.size(), "file");
        }
    }

    private static List<File> toFiles(Collection<Path> paths) {
        List<File> result = new LinkedList<>();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                JkPathTree.of(path).andMatching(true, "**/*.java").stream().forEach(pat -> result.add(pat.toFile()));
            } else {
                result.add(path.toFile());
            }
        }
        return result;
    }

    private static boolean runOnTool(JkJavaCompileSpec compileSpec, JavaCompiler compiler, String[] toolOptions) {
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        List<File> files = toFiles(compileSpec.getSourceFiles());
        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
        List<String> options = new LinkedList<>();
        options.addAll(Arrays.asList(toolOptions));
        options.addAll(compileSpec.getOptions());
        CompilationTask task = compiler.getTask(new PrintWriter(JkLog.getOutputStream()),
                null, new JkDiagnosticListener(), options, null, javaFileObjects);
        return task.call();
    }

    private static boolean runOnProcess(JkJavaCompileSpec compileSpec, JkProcess process) {
        JkLog.info("Compile using command " + process.getCommand());
        final List<String> sourcePaths = new LinkedList<>();
        List<Path> paths = compileSpec.getSourceFiles();
        for (final Path file : paths) {
            if (Files.isDirectory(file)) {
                JkPathTree.of(file).andMatching(true, "**/*.java").stream()
                        .forEach(path -> sourcePaths.add(path.toString()));
            } else {
                sourcePaths.add(file.toAbsolutePath().toString());
            }
        }
        final JkProcess jkProcess = process.andParams(compileSpec.getOptions()).andParams(sourcePaths);
        JkLog.info("" + sourcePaths.size() + " files to compile.");
        final int result = jkProcess.runSync();
        return (result == 0);
    }

    private static JavaCompiler getDefaultOrFail() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JkUtilsAssert.state(compiler != null, "This platform does not provide compileRunner. " +
                "Try another JDK or use JkJavaCompiler.andCompiler(JavaCompiler)");
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

    private boolean runCompiler(JkJavaCompileSpec compileSpec) {
        JkJavaVersion runningJdkVersion = JkJavaVersion.of(runningJdkVersion());
        if (compileTool != null) {
            if (compileSpec.getSourceVersion() == null
                    || canCompile(compileTool, compileSpec.getSourceVersion())) {
                return runOnTool(compileSpec, compileTool, toolOptions);
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
            if (forkOptions == null) {
                return runOnTool(compileSpec, runningJdkCompilerTool, toolOptions);
            } else {
                return runOnProcess(compileSpec, JkProcess.ofJavaTool("javac", forkOptions()));
            }
        }
        JkProcess process = resolveCompileProcessFromJdksForVersion(compileSpec.getSourceVersion());
        if (process != null) {
            return runOnProcess(compileSpec, process.andParams(forkOptions()));
        }
        if (canCompile(runningJdkCompilerTool, compileSpec.getSourceVersion())) {
            return runOnTool(compileSpec, runningJdkCompilerTool, toolOptions);
        }
        throw new IllegalStateException("Cannot find suitable JDK to compile version "
                + compileSpec.getSourceVersion()
            + "\nRunning JDK is " + runningJdkVersion + " and known JDK homes are " + this.jdkHomes);
    }

    private static boolean canCompile(JavaCompiler compilerTool, JkJavaVersion version) {
        return compilerTool.getSourceVersions().stream()
                .map(sourceVersion -> JkUtilsString.substringAfterFirst(sourceVersion.name(), "RELEASE_"))
                .map(versionNum -> JkJavaVersion.of(versionNum))
                .findAny().isPresent();
    }

    private String[] forkOptions() {
        return forkOptions == null ? new String[0] : forkOptions;
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
