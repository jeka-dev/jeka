package org.jerkar.api.java;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Stand for a compilation setting and process. Use this class to perform java
 * compilation.
 */
public final class JkJavaCompiler {

    private static final String OUTPUR_DIR_OPTS = "-d";

    private static final String CLASSPATH_OPTS = "-cp";


    /** Filter to retain only source files */
    public static final JkPathFilter JAVA_SOURCE_ONLY_FILTER = JkPathFilter.include("**/*.java");


    /**
     * Creates a {@link JkJavaCompiler} producing its output in the given
     * directory.
     */
    @SuppressWarnings("unchecked")
    public static JkJavaCompiler base() {
        return new JkJavaCompiler(Collections.EMPTY_LIST, Collections.EMPTY_LIST, true, null,
                null, new HashMap<>());
    }
    /**
     * Creates a {@link JkJavaCompiler} producing its output in the given
     * directory.
     */
    @SuppressWarnings("unchecked")
    public static JkJavaCompiler outputtingIn(File outputDir) {
        return base().withOutputDir(outputDir);
    }

    private final List<String> options;

    private final List<File> javaSourceFiles;

    private final boolean failOnError;

    private final JkProcess fork;

    private final JavaCompiler compiler;

    private final Map<JkJavaVersion, File> compilerBinRepo;

    private JkJavaCompiler(List<String> options, List<File> javaSourceFiles, boolean failOnError,
            JkProcess fork, JavaCompiler compiler, Map<JkJavaVersion, File> compilerBinRepo) {
        super();
        this.options = options;
        this.javaSourceFiles = javaSourceFiles;
        this.failOnError = failOnError;
        this.fork = fork;
        this.compiler = compiler;
        this.compilerBinRepo = compilerBinRepo;
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified
     * failed-on-error parameter. If <code>true</code> then
     * a compilation error will throw a {@link IllegalStateException}.
     */
    public JkJavaCompiler failOnError(boolean fail) {
        return new JkJavaCompiler(options, javaSourceFiles, fail, fork, compiler, compilerBinRepo);
    }


    /**
     * Creates a copy of this {@link JkJavaCompiler} but adding the specified
     * options. Options are option you pass in javac command line as
     * -deprecation, -nowarn, ... For example, if you want something equivalent
     * to javac -deprecation -cp path1 path2, you should pass "-deprecation",
     * "-cp", "path1", "path2" parameters (all space separated words must stands
     * for one parameter, in other words : parameters must not contain any
     * space).
     */
    public JkJavaCompiler andOptions(String... options) {
        final List<String> newOptions = new LinkedList<String>(this.options);
        newOptions.addAll(Arrays.asList(options));
        return new JkJavaCompiler(newOptions, javaSourceFiles, failOnError, fork, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified
     * options.
     */
    public JkJavaCompiler andOptions(Collection<String> options) {
        final List<String> newOptions = new LinkedList<String>(this.options);
        newOptions.addAll(options);
        return new JkJavaCompiler(newOptions, javaSourceFiles, failOnError, fork, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified
     * options under condition.
     */
    public JkJavaCompiler andOptionsIf(boolean condition, String... options) {
        if (condition) {
            return andOptions(options);
        }
        return this;
    }

    /**
     * Some options of a compiler are set in a couple of name/value (version, classpath, .....).
     * So if you want to explicitly set such an option it is desirable to remove current value
     * instead of adding it at the queue of options.
     */
    public JkJavaCompiler withOption(String optionName, String optionValue) {
        final List<String> newOptions = JkJavaCompilerSpec.addOrReplace(options, optionName, optionValue);
        return new JkJavaCompiler(newOptions, javaSourceFiles, failOnError, fork, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this compiler but outputting in the specified directory.
     */
    public JkJavaCompiler withOutputDir(File outputDir) {
        if (outputDir.exists() && !outputDir.isDirectory()) {
            throw new IllegalArgumentException(outputDir.getAbsolutePath() + " is not a directory.");
        }
        return withOption(OUTPUR_DIR_OPTS, outputDir.getAbsolutePath());
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified
     * classpath.
     */
    public JkJavaCompiler withClasspath(Iterable<File> files) {
        if (!files.iterator().hasNext()) {
            return this;
        }
        final String classpath = JkClasspath.of(files).toString();
        return this.withOption(CLASSPATH_OPTS, classpath);
    }

    private File getOutputDir() {
        String path = JkJavaCompilerSpec.findValueAfter(options, OUTPUR_DIR_OPTS);
        return path == null ? null : new File(path);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with forking the javac
     * process. The javac process is created using specified argument defined in
     * {@link JkProcess#ofJavaTool(String, String...)}
     */
    public JkJavaCompiler fork(String... parameters) {
        return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError,
                JkProcess.ofJavaTool("javac", parameters), compiler, compilerBinRepo);
    }

    /**
     * As {@link #fork(String...)} but the fork is actually done only if the
     * <code>fork</code> parameter is <code>true</code>.
     */
    public JkJavaCompiler fork(boolean fork, String... parameters) {
        if (fork) {
            return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles,
                    failOnError, JkProcess.ofJavaTool("javac", parameters), compiler, compilerBinRepo);
        } else {
            return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles,
                    failOnError, null, compiler, compilerBinRepo);
        }
    }

    /**
     * As {@link #fork(String...)} but specifying the executable for the
     * compiler.
     *
     * @param executable
     *            The executable for the compiler as 'jike' or
     *            '/my/special/jdk/javac'
     */
    public JkJavaCompiler forkOnCompiler(String executable, String... parameters) {
        return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError,
                JkProcess.of(executable, parameters), compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but adding specified source
     * files.
     */
    public JkJavaCompiler andSources(Iterable<File> files) {
        final List<File> newSources = new LinkedList<File>(this.javaSourceFiles);
        for (final File file : files) {
            if (file.getName().toLowerCase().endsWith(".java")) {
                newSources.add(file);
            }
        }
        return new JkJavaCompiler(options, newSources, failOnError, fork, compiler, compilerBinRepo);
    }

    /**
     * @see #andSources(Iterable)
     */
    public JkJavaCompiler andSourceDir(File dir) {
        return andSources(JkFileTree.of(dir));
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but with the specified compiler instance.
     * Since in-process compilers cannot be run in a forked process, this method disables any
     * previous fork options that may have been set.
     */
    public JkJavaCompiler withCompiler(JavaCompiler compiler) {
        // turn off forking
        return new JkJavaCompiler(options, javaSourceFiles, failOnError, null, compiler, compilerBinRepo);
    }

    /**
     * Creates a copy of this {@link JkJavaCompiler} but adding an external java compiler for
     * the specified source version. The compiler will try to get compliant compiler to
     * compile source.
     */
    public JkJavaCompiler withJavacBin(JkJavaVersion version, File javacBin) {
        HashMap<JkJavaVersion, File> map = new HashMap<>(this.compilerBinRepo);
        map.put(version, javacBin);
        return new JkJavaCompiler(options, javaSourceFiles, failOnError, fork, compiler, map);
    }

    /**
     * Actually compile the source files to the output directory.
     *
     * @return <code>false</code> if a compilation error occurred.
     *
     * @throws if
     *             a compilation error occured and the 'failOnError' flag in on.
     */
    @SuppressWarnings("unchecked")
    public boolean compile() {
        File outputDir = this.getOutputDir();
        if (outputDir == null) {
            throw new IllegalStateException("Output dir option (-d) has not been specified on the compiler. Specified options : " + options);
        }
        this.getOutputDir().mkdirs();
        final JavaCompiler compiler = this.compiler != null ? this.compiler : getDefaultOrFail();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                null);
        String message = "Compiling " + javaSourceFiles.size() + " source files";
        if (JkLog.verbose()) {
            message = message + " using options : " + JkUtilsString
                    .join(options, " ");
        }
        JkLog.startln(message);
        if (javaSourceFiles.isEmpty()) {
            JkLog.warn("No source to compile");
            JkLog.done();
            return true;
        }
        final boolean result;
        if (this.fork == null) {
            final Iterable<? extends JavaFileObject> javaFileObjects = fileManager
                    .getJavaFileObjectsFromFiles(this.javaSourceFiles);
            final CompilationTask task = compiler.getTask(new PrintWriter(JkLog.warnStream()),
                    null, new JkDiagnosticListener(), options, null, javaFileObjects);
            result = task.call();
        } else {
            result = runOnFork();
        }
        JkLog.done();
        if (!result) {
            if (failOnError) {
                throw new IllegalStateException("Compilation failed.");
            }
            return false;
        }
        return true;
    }

    private boolean runOnFork() {
        final List<String> sourcePaths = new LinkedList<String>();
        for (final File file : javaSourceFiles) {
            sourcePaths.add(file.getAbsolutePath());
        }
        final JkProcess jkProcess = this.fork.andParameters(options).andParameters(sourcePaths);
        final int result = jkProcess.runSync();
        return (result == 0);
    }

    private static JavaCompiler getDefaultOrFail() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("This platform does not provide compiler. Try another JDK or use JkJavaCompiler.andCompiler(JavaCompiler)");
        }
        return compiler;
    }

    static String currentJdkSourceVersion() {
        final String fullVersion = System.getProperty("java.version");
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
     * Returns a {@link JkJavaCompiler} identical to this one but within
     * a forked process with relevant JDK if this specified source version
     * does not match with the current running JDK. The specified map may include
     * the JDK location for this source version.
     * The keys must be formatted as "jdk.[source version]". For example, "jdk.1.4" or
     * "jdk.7". The values must absolute path.
     */
    public JkJavaCompiler forkedIfNeeded(Map<String, String> jdkLocations) {
        String versionCache = JkJavaCompilerSpec.findValueAfter(this.options, JkJavaCompilerSpec.SOURCE_OPTS);
        if (versionCache == null) {
            return this;
        }
        if (versionCache.equals(currentJdkSourceVersion())) {
            JkLog.info("Current JDK matches with source version (" + versionCache + "). Don't need to fork.");
            return this;
        }
        final String key = "jdk." + versionCache;
        final String path = jdkLocations.get(key);
        if (path == null) {
            JkLog.warn("Current JDK does not match with source version " + versionCache + ".",
                    " No exact matching JDK found for version " + versionCache + ".",
                    " Will use the current one which is version " + currentJdkSourceVersion() + ".",
                    " Pass option -jdk." + versionCache + "=[JDK location] to specify the JDK to use for Java version " + versionCache);
            return this;
        }
        final String cmd = path + "/bin/javac";
        JkLog.info("Current JDK does not match with source version (" + versionCache + "). Will use JDK "
                + path);
        final JkProcess process = JkProcess.of(cmd);
        return new JkJavaCompiler(options, javaSourceFiles, failOnError, process, compiler, compilerBinRepo);
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
