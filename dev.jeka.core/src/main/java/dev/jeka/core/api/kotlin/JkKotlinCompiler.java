package dev.jeka.core.api.kotlin;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Stand for a compilation setting and process. Use this class to perform java
 * compilation.
 */
public final class JkKotlinCompiler {

    private static final String KOTLIN_HOME = "KOTLIN_HOME";

    private boolean failOnError = true;

    private final JkProcess process;

    private final List<String> options = new LinkedList<>();

    private JkKotlinCompiler(JkProcess process) {
        super();
        this.process = process;
    }

    /**
     * Creates a {@link JkKotlinCompiler} based on the default kotlin compiler installed on the host machine.
     */
    public static JkKotlinCompiler ofDefault() {
        JkProcess process = JkProcess.of("kotlinc");
        return new JkKotlinCompiler(process);
    }

    public static JkKotlinCompiler ofKotlinHome() {
        String value = System.getenv("KOTLIN_HOME");
        JkUtilsAssert.state(value != null, KOTLIN_HOME + " environment variable is not defined. Please define this environment variable in order to compile Kotlin sources.");
        String command = value + File.separator + "bin" + File.separator + "kotlinc-jvm";
        if (JkUtilsSystem.IS_WINDOWS) {
            command = command + ".bat";
        }
        return new JkKotlinCompiler(JkProcess.of(command));

    }

    public Path getStdLib() {
        String value = System.getenv("KOTLIN_HOME");
        JkUtilsAssert.state(value != null, KOTLIN_HOME + " environment variable is not defined.");
        return Paths.get(value).resolve("libexec/lib/kotlin-stdlib.jar");
    }

    public JkKotlinCompiler setFailOnError(boolean fail) {
        this.failOnError = fail;
        return this;
    }

    public JkKotlinCompiler addOption(String option) {
        this.options.add(option);
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
    public boolean compile(JkKotlinJvmCompileSpec compileSpec) {
        final Path outputDir = compileSpec.getOutputDir();
        List<String> options = compileSpec.getOptions();
        if (outputDir == null) {
            throw new IllegalStateException("Output dir option (-d) has not been specified on the compiler. Specified options : " + options);
        }
        JkUtilsPath.createDirectories(outputDir);
        String message = "Compiling Kotlin " + compileSpec.getSourceFiles() + " source files";
        if (JkLog.verbosity().isVerbose()) {
            message = message + " to " + outputDir + " using options : " + String.join(" ", options);
        }
        long start = System.nanoTime();
        JkLog.startTask(message);
        if (compileSpec.getSourceFiles().isEmpty()) {
            JkLog.warn("No source to compile");
            JkLog.endTask("");
            return true;
        }
        JkLog.info("Use kotlin compiler : " + process.getCommand());
        final Result result = run(compileSpec);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        if (!result.success) {
            if (failOnError) {
                throw new IllegalStateException("Kotlin compiler failed " + result.params);
            }
            return false;
        }
        return true;
    }

    private Result run(JkKotlinJvmCompileSpec compileSpec) {
        final List<String> sourcePaths = new LinkedList<>();
        for (final Path file : compileSpec.getSourceFiles()) {
            if (Files.isDirectory(file)) {
                Path workingdir = Optional.ofNullable(process.getWorkingDir()).orElse(Paths.get(""));
                JkPathTree.of(file).andMatching(true, "**/*.kt", "*.kt").stream()
                        .forEach(path -> sourcePaths.add(workingdir.toAbsolutePath().relativize(path).toString()));
            } else {
                sourcePaths.add(file.toAbsolutePath().toString());
            }
        }
        if (sourcePaths.isEmpty()) {
            JkLog.warn("No Kotlin source found in " + compileSpec.getSourceFiles());
            return new Result(true, Collections.emptyList());
        }
        final JkProcess jkProcess = this.process.clone()
                .addParams(compileSpec.getOptions())
                .addParams(options)
                .addParams(sourcePaths);
        JkLog.info("" + sourcePaths.size() + " files to compile.");
        final int result = jkProcess.exec();
        return new Result(result == 0, jkProcess.getParams());
    }

    private static class Result {
        final boolean success;
        final List<String> params;

        public Result(boolean success, List<String> params) {
            this.success = success;
            this.params = params;
        }
    }

}
