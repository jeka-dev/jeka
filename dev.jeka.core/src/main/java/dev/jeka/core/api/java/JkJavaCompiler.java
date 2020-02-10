package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkException;
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
import java.util.*;

/**
 * Stand for a compilation setting and process. Use this class to perform java
 * compilation.
 */
public final class JkJavaCompiler {

    private final boolean failOnError;

    private final JkProcess fork;

    private final JavaCompiler compiler;

    private final Map<JkJavaVersion, Path> compilerBinRepo;

    private JkJavaCompiler(boolean failOnError,
            JkProcess fork, JavaCompiler compiler, Map<JkJavaVersion, Path> compilerBinRepo) {
        super();
        this.failOnError = failOnError;
        this.fork = fork;
        this.compiler = compiler;
        this.compilerBinRepo = compilerBinRepo;
    }

    /**
     * Creates a {@link JkJavaCompiler} producing its output in the given directory.
     */
    public static JkJavaCompiler ofJdk() {
        return new JkJavaCompiler(true, null, null, new HashMap<>());
    }

    public static JkJavaCompiler of(JavaCompiler compiler) {
        return new JkJavaCompiler(true, null, compiler, new HashMap<>());
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified
     * failed-on-error parameter. If <code>true</code> then
     * a compilation error will throw a {@link IllegalStateException}.
     */
    public JkJavaCompiler withFailOnError(boolean fail) {
        return new JkJavaCompiler(fail, fork, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with forking the javac
     * process. The javac process is created using specified argument defined in
     * {@link JkProcess#ofJavaTool(String, String...)}
     */
    public JkJavaCompiler withForking(String... parameters) {
        return withForking(JkProcess.ofJavaTool("javac", parameters));
    }

    /**
     * As {@link #withForking(String...)} but the fork is actually done only if the
     * <code>fork</code> parameter is <code>true</code>.
     */
    public JkJavaCompiler withForking(boolean fork, String... parameters) {
        if (fork) {
            return withForking(parameters);
        }
        return withForking((JkProcess) null);
    }

    public JkJavaCompiler withForking(JkProcess compileProcess) {
        return new JkJavaCompiler(failOnError, compileProcess , compiler, compilerBinRepo);
    }

    /**
     * As {@link #withForking(String...)} but specifying the executable for the compileRunner.
     *
     * @param executable The executable for the compileRunner as 'jike' or '/my/specific/jdk/javac'
     */
    public JkJavaCompiler withForkingOnCompiler(String executable, String... parameters) {
        final JkProcess compileProcess = JkProcess.of(executable, parameters);
        return withForking(compileProcess);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified compileRunner instance.
     * Since in-process compilers cannot be run andAccept a withForking process, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompiler withCompiler(JavaCompiler compiler) {
        return new JkJavaCompiler(failOnError, null, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but adding an external java compileRunner for
     * the specified source version. The compileRunner will try to get compliant compileRunner to
     * compile source.
     */
    public JkJavaCompiler withJavacBin(JkJavaVersion version, Path javacBin) {
        final HashMap<JkJavaVersion, Path> map = new HashMap<>(this.compilerBinRepo);
        map.put(version, javacBin);
        return new JkJavaCompiler(failOnError, fork, compiler, map);
    }

    /**
     * Returns <code>true</code> if no compiler or fork has been set on.
     */
    public boolean isDefault() {
        return compiler == null && fork == null;
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
        final JavaCompiler compiler = this.compiler != null ? this.compiler : getDefaultOrFail();
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
            JkLog.endTask("");
            return true;
        }
        final boolean result;
        if (this.fork == null) {
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
            JkLog.info("Use a forking process to perform compilation : " + fork.getCommand());
            result = runOnFork(compileSpec);
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        if (!result) {
            if (failOnError) {
                throw new JkException("Compilation failed with options " + options);
            }
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
        for (final Path file : compileSpec.getSourceFiles()) {
            if (Files.isDirectory(file)) {
                JkPathTree.of(file).andMatching(true, "**/*.java").stream().forEach(path -> sourcePaths.add(path.toString()));
            } else {
                sourcePaths.add(file.toAbsolutePath().toString());
            }
        }
        final JkProcess jkProcess = this.fork.andParams(compileSpec.getOptions()).andParams(sourcePaths);
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
