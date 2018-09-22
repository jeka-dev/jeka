package org.jerkar.api.java;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.system.JkException;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;

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
    public static JkJavaCompiler of() {
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
    public JkJavaCompiler failOnError(boolean fail) {
        return new JkJavaCompiler(fail, fork, compiler, compilerBinRepo);
    }


    /**
     * Creates a copy of this {@link JkJavaCompiler} but with forking the javac
     * process. The javac process is created using specified argument defined in
     * {@link JkProcess#ofJavaTool(String, String...)}
     */
    public JkJavaCompiler fork(String... parameters) {
        return new JkJavaCompiler(failOnError,
                JkProcess.ofJavaTool("javac", parameters), compiler, compilerBinRepo);
    }

    /**
     * As {@link #fork(String...)} but the fork is actually done only if the
     * <code>fork</code> parameter is <code>true</code>.
     */
    public JkJavaCompiler fork(boolean fork, String... parameters) {
        if (fork) {
            final JkProcess compileProcess = JkProcess.ofJavaTool("javac", parameters);
            return new JkJavaCompiler(failOnError, compileProcess , compiler, compilerBinRepo);
        } else {
            return new JkJavaCompiler(failOnError, null, compiler, compilerBinRepo);
        }
    }

    /**
     * As {@link #fork(String...)} but specifying the executable for the compileRunner.
     *
     * @param executable The executable for the compileRunner as 'jike' or '/my/specific/jdk/javac'
     */
    public JkJavaCompiler forkOnCompiler(String executable, String... parameters) {
        final JkProcess compileProcess = JkProcess.of(executable, parameters);
        return new JkJavaCompiler(failOnError, compileProcess, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified compileRunner instance.
     * Since in-process compilers cannot be run accept a forked process, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompiler withCompiler(JavaCompiler compiler) {
        return new JkJavaCompiler(failOnError, null, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but adding an external java compileRunner for
     * the specified source projectVersion. The compileRunner will try to get compliant compileRunner to
     * compile source.
     */
    public JkJavaCompiler withJavacBin(JkJavaVersion version, Path javacBin) {
        final HashMap<JkJavaVersion, Path> map = new HashMap<>(this.compilerBinRepo);
        map.put(version, javacBin);
        return new JkJavaCompiler(failOnError, fork, compiler, map);
    }

    /**
     * Actually compile the source files to the output directory.
     *
     * @return <code>false</code> if a compilation error occurred.
     *
     * @throws IllegalStateException if a compilation error occurred and the 'failOnError' flag is <code>true</code>.
     */
    @SuppressWarnings("unchecked")
    public boolean compile(JkJavaCompileSpec compileSpec) {
        final Path outputDir = compileSpec.getOutputDir();
        List<String> options = compileSpec.getOptions();
        if (outputDir == null) {
            throw new IllegalStateException("Output dir option (-d) has not been specified on the compileRunner. Specified options : " + options);
        }
        JkUtilsPath.createDirectories(outputDir);
        final JavaCompiler compiler = this.compiler != null ? this.compiler : getDefaultOrFail();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                null);
        String message = "Compiling " + compileSpec.getSourceFiles() + " source files";
        if (JkLog.isVerbose()) {
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
            final CompilationTask task = compiler.getTask(new PrintWriter(JkLog.stream()),
                    null, new JkDiagnosticListener(), options, null, javaFileObjects);
            if (files.size() > 0) {
                JkLog.info("" + files.size() + " files to compile.");
                result = task.call();
            } else {
                JkLog.warn("" + files.size() + " files to compile.");
                JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
                return true;
            }
        } else {
            JkLog.info("Use a forked process to perform compilation : " + fork.commandName());
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
                JkPathTree.of(path).andAccept("**/*.java").stream().forEach(pat -> result.add(pat.toFile()));
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
                JkPathTree.of(file).andAccept("**/*.java").stream().forEach(path -> sourcePaths.add(path.toString()));
            } else {
                sourcePaths.add(file.toAbsolutePath().toString());
            }
        }
        final JkProcess jkProcess = this.fork.andParameters(compileSpec.getOptions()).andParameters(sourcePaths);
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
        final String fullVersion = System.getProperty("java.projectVersion");
        final int firstDot = fullVersion.indexOf(".");
        final String version;
        if (firstDot == -1 ) {
            version = fullVersion;
        } else {
            final int secondDot = fullVersion.indexOf(".", firstDot+1);
            if (secondDot == -1) {
                version = fullVersion;
            } else {
                version = fullVersion.substring(0, secondDot);
            }
        }
        if (version.equals(JkJavaVersion.V1_3.name()) || version.equals(JkJavaVersion.V1_4.name())) {
            return version;
        }
        String shortVersion;
        if (firstDot != -1) {
            shortVersion = version.substring(firstDot+1);
        } else {
            shortVersion = version;
        }
        return shortVersion;
    }

    /**
     * Returns a {@link JkProcess} standing for a forked compileRunner with relevant JDK if this specified source projectVersion
     * does not match with the current running JDK. The specified map may include
     * the JDK location for this source projectVersion.
     * If no need to fork, cause current JDK is aligned with target projectVersion, then yhis method returns <code>null</code>.
     * The keys must be formatted as "jdk.[source projectVersion]". For example, "jdk.1.4" or
     * "jdk.7". The values must absolute path.
     */
    public JkProcess forkedIfNeeded(Map<String, String> jdkLocations, String version) {
        if (version.equals(currentJdkSourceVersion())) {
            JkLog.info("Current JDK matches with source projectVersion (" + version + "). Don't need to fork.");
            return null;
        }
        final String key = "jdk." + version;
        final String path = jdkLocations.get(key);
        if (path == null) {
            JkLog.warn("Current JDK does not match with source projectVersion " + version + ".\n" +
                    " No exact matching JDK found for projectVersion " + version + ".\n" +
                    " Will use the current one which is projectVersion " + currentJdkSourceVersion() + ".\n" +
                    " Pass option -jdk." + version + "=[JDK location] to specify the JDK to use for Java projectVersion " + version);
            return null;
        }
        final String cmd = path + "/bin/javac";
        JkLog.info("Current JDK does not match with source projectVersion (" + version + "). Will use JDK "
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
