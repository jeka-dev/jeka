package dev.jeka.core.api.kotlin;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Stand for a compilation setting and process. Use this class to perform java
 * compilation.
 */
public final class JkKotlinCompiler {

    private static final String KOTLIN_HOME = "KOTLIN_HOME";

    private final boolean failOnError;

    private final JkProcess process;

    private JkKotlinCompiler(boolean failOnError,
                             JkProcess process) {
        super();
        this.failOnError = failOnError;
        this.process = process;
    }

    /**
     * Creates a {@link JkKotlinCompiler} based on the default kotlin compiler installed on the host machine.
     */
    public static JkKotlinCompiler ofDefault() {
        JkProcess process = JkProcess.of("kotlinc");
        return new JkKotlinCompiler(true, process);
    }

    public static JkKotlinCompiler ofKotlinHome() {
        String value = System.getenv("KOTLIN_HOME");
        JkUtilsAssert.notNull(value, KOTLIN_HOME + " environment variable is not defined.");
        String command = value + "/bin/kotlinc-jvm";
        return new JkKotlinCompiler(true, JkProcess.of(command));

    }

    public Path getStdLib() {
        String value = System.getenv("KOTLIN_HOME");
        JkUtilsAssert.notNull(value, KOTLIN_HOME + " environment variable is not defined.");
        return Paths.get(value).resolve("libexec/lib/kotlin-stdlib.jar");
    }

    /**
     * Creates a copy of this {@link JkKotlinCompiler} but with the specified
     * failed-on-error parameter. If <code>true</code> then
     * a compilation error will throw a {@link IllegalStateException}.
     */
    public JkKotlinCompiler withFailOnError(boolean fail) {
        return new JkKotlinCompiler(fail, process);
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
        final Path outputDir = compileSpec.getOutputDir();
        List<String> options = compileSpec.getOptions();
        if (outputDir == null) {
            throw new IllegalStateException("Output dir option (-d) has not been specified on the compiler. Specified options : " + options);
        }
        JkUtilsPath.createDirectories(outputDir);
        String message = "Compiling Kotlin " + compileSpec.getSourceFiles() + " source files";
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
        JkLog.info("Use kotlin compiler : " + process.getCommand());
        final boolean result = run(compileSpec);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        if (!result) {
            if (failOnError) {
                throw new JkException("Compilation failed with options " + options);
            }
            return false;
        }
        return true;
    }

    private boolean run(JkKotlinJvmCompileSpec compileSpec) {
        final List<String> sourcePaths = new LinkedList<>();
        for (final Path file : compileSpec.getSourceFiles()) {
            if (Files.isDirectory(file)) {
                JkPathTree.of(file).andMatching(true, "**/*.kt").stream()
                        .forEach(path -> sourcePaths.add(path.toString()));
            } else {
                sourcePaths.add(file.toAbsolutePath().toString());
            }
        }
        final JkProcess jkProcess = this.process.andParams(compileSpec.getOptions()).andParams(sourcePaths);
        JkLog.info("" + sourcePaths.size() + " files to compile.");
        final int result = jkProcess.runSync();
        return (result == 0);
    }

}
