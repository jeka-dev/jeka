package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Compiler for Java source code. Underlying, it uses either a {@link JavaCompiler} instance either an external
 * process (forked mode).
 */
public final class JkJavaCompiler<T> {

    private JkProcess forkingProcess;

    private JavaCompiler compilerTool;

    /**
     * Owner for parent chaining
     */
    public final T _;


    private JkJavaCompiler(T _) {
        this._ = _;
    }

    /**
     * Creates a {@link JkJavaCompiler} without specifying a {@link JavaCompiler} instance or an external process.
     * When nothing is specified, this compiler will try the default {@link JavaCompiler} instance provided
     * by the running JDK.
     */
    public static JkJavaCompiler<Void> of() {
        return new JkJavaCompiler(null);
    }

    /**
     * Same as {@link #of()} but mentioning a owner for parent chaining.
     */
    public static <T> JkJavaCompiler<T> of(T parent) {
        return new JkJavaCompiler(parent);
    }

    /**
     * Sets underlying java compiler tool to use.
     * Since in-process compilers cannot be run in forking process mode, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompiler<T> setCompilerTool(JavaCompiler compiler) {
        this.compilerTool = compiler;
        this.forkingProcess = null;
        return this;
    }

    /**
     * Sets the underlying compiler with the specified process. The process is typically a 'javac' command.
     */
    public JkJavaCompiler<T> setForkingProcess(JkProcess compileProcess) {
        this.forkingProcess = compileProcess;
        return this;
    }

    /**
     * Sets to default underlying compiler, meaning the compiler tool embedded in the running JDK
     */
    public JkJavaCompiler<T> setDefault() {
        this.forkingProcess = null;
        this.compilerTool = null;
        return this;
    }

    /**
     * Set the underlying compiler as an external process of the 'javac' tool provided with the running JDK.
     * @see #setForkingProcess(JkProcess)
     */
    public JkJavaCompiler<T> setForkingWithJavac(String... parameters) {
        return setForkingProcess(JkProcess.ofJavaTool("javac", parameters));
    }

    /**
     * Same as {@link #setForkingWithJavac(String...)} but only operates if the specified condition is {@code true}
     */
    public JkJavaCompiler<T> setForkingWithJavacIf(boolean condition, String... parameters) {
        if (condition) {
            return setForkingWithJavac(parameters);
        }
        return this;
    }


    /**
     * Returns <code>true</code> if no compiler or fork has been set on.
     */
    public boolean isDefault() {
        return compilerTool == null && forkingProcess == null;
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
            throw new IllegalStateException("Output dir option (-d) has not been specified on the compiler. Specified options : " + options);
        }
        JkUtilsPath.createDirectories(outputDir);
        final JavaCompiler compiler = this.compilerTool != null ? this.compilerTool : getDefaultOrFail();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                null);
        String message = "Compiling " + compileSpec.getSourceFiles() + " source files";
        if (JkLog.verbosity().isVerbose()) {
            message = message + " to " + outputDir + " using options : " + JkUtilsString
                    .join(options, " ");
        }
        long start = System.nanoTime();
        JkLog.startTask(message);
        if (compileSpec.getSourceFiles().isEmpty()) {
            JkLog.warn("No source to compile");
            JkLog.endTask();
            return true;
        }
        final boolean result;
        if (this.forkingProcess == null) {
            List<File> files = toFiles(compileSpec.getSourceFiles());
            final Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjectsFromFiles(files);
            final CompilationTask task = compiler.getTask(new PrintWriter(JkLog.getOutputStream()),
                    null, new JkDiagnosticListener(), options, null, javaFileObjects);
            if (files.size() > 0) {
                JkLog.info("" + files.size() + " files to compile with " + compiler.getClass().getSimpleName());

                result = task.call();
            } else {
                JkLog.warn("No file to compile.");
                JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
                return true;
            }
        } else {
            JkLog.info("Use a forking process to perform compilation : " + forkingProcess.getCommand());
            result = runOnFork(compileSpec);
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        if (!result) {
            return false;
        }
        return true;
    }

    private List<File> toFiles(Collection<Path> paths) {
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

    private boolean runOnFork(JkJavaCompileSpec compileSpec) {
        final List<String> sourcePaths = new LinkedList<>();
        List<Path> paths = compileSpec.getSourceFiles();
        for (final Path file : paths) {
            if (Files.isDirectory(file)) {
                JkPathTree.of(file).andMatching(true, "**/*.java").stream().forEach(path -> sourcePaths.add(path.toString()));
            } else {
                sourcePaths.add(file.toAbsolutePath().toString());
            }
        }
        final JkProcess jkProcess = this.forkingProcess.andParams(compileSpec.getOptions()).andParams(sourcePaths);
        JkLog.info("" + sourcePaths.size() + " files to compile.");
        final int result = jkProcess.runSync();
        return (result == 0);
    }

    private static JavaCompiler getDefaultOrFail() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("This platform does not provide compileRunner. Try another JDK or use JkJavaCompiler.andCompiler(JavaCompiler)");
        }
        return compiler;
    }

    static String currentJdkSourceVersion() {
        final String fullVersion = System.getProperty("java.version");
        return currentJdkSourceVersion(fullVersion);
    }

    static String currentJdkSourceVersion(String fullVersion) {
        String[] items = fullVersion.split("\\.");
        if (items.length == 1 ) {
            return fullVersion;
        }
        if ("1".equals(items[0])) {
            return items[1];
        }
        return items[0];
    }

    /**
     * Returns a {@link JkProcess} standing for a forking compiler with relevant JDK if this specified source version
     * does not match with the current running JDK. The specified map may include
     * the JDK location for this source version.
     * If no need to fork, cause current JDK is aligned with target version, then this method returns <code>null</code>.
     * The keys must be formatted as "jdk.[source version]". For example, "jdk.1.4" or
     * "jdk.7". The values must absolute path.
     */
    public static JkProcess getForkedProcessOnJavaSourceVersion(Map<String, String> jdkLocations, String version) {
        if (version.equals(currentJdkSourceVersion())) {
            JkLog.info("Current JDK matches with source version (" + version + "). Don't need to fork.");
            return null;
        }
        final String key = "jdk." + version;
        final String path = jdkLocations.get(key);
        if (path == null) {
            JkLog.warn("Current JDK does not match with source version " + version + ".\n" +
                    " No exact matching JDK found for version " + version + ".\n" +
                    " Will use the current one which is version " + currentJdkSourceVersion() + ".\n" +
                    " Pass option -jdk." + version + "=[JDK location] to specify the JDK to use for Java version " + version);
            return null;
        }
        final String cmd = path + "/bin/javac";
        JkLog.info("Current JDK does not match with source version (" + version + "). Will use JDK "
                + path);
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
