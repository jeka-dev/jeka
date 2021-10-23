package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsSystem;

import javax.tools.DocumentationTool;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Provides fluent interface for producing Javadoc.
 *
 * @author Jerome Angibaud
 */
public final class JkJavadocProcessor<T> {

    //private final JkPathTreeSet srcDirs;

    private final List<String> options = new LinkedList<>();

    //private final Iterable<Path> classpath;

    //private final Path outputDir;

    private Boolean displayOutput;

    /**
     * For parent chaining
     */
    public final T __;


    private JkJavadocProcessor(T parent) {
        this.__ = parent;
    }

    /**
     * Creates a default {@link JkJavadocProcessor} .
     */
    public static JkJavadocProcessor<Void> of() {
        return ofParent(null);
    }

    /**
     * Sale as {@link #of()} but providing a parent chaining
     */
    public static <T> JkJavadocProcessor<T> ofParent(T parent) {
        return new JkJavadocProcessor(parent);
    }

    /**
     * Returns the axtra arguments passed to the Javadoc tool.
     */
    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    /**
     * Adds the specified parameters to Javadoc tool.
     */
    public JkJavadocProcessor<T> addOptions(String ... options) {
        return addOptions(Arrays.asList(options));
    }

    /**
     * @see #addOptions(String...)
     */
    public JkJavadocProcessor<T> addOptions(Iterable<String> options) {
        JkUtilsIterable.addAllWithoutDuplicate(this.options, options);
        return this;
    }

    public Boolean getDisplayOutput() {
        return displayOutput;
    }

    public JkJavadocProcessor<T> setDisplayOutput(Boolean displayOutput) {
        this.displayOutput = displayOutput;
        return this;
    }

    /**
     * Actually processes and creates the javadoc files.
     */
    public void make(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        JkLog.startTask("Generate javadoc");
        if (srcDirs.hasNoExistingRoot()) {
            JkLog.warn("No sources found in " + srcDirs);
            return;
        }
        //executeTool(outputDir);
        if (srcDirs.count(1, false) > 0) {
            executeCommandLine(classpath, srcDirs, outputDir);
        } else {
            JkLog.warn("No source file detected. Skip Javadoc.");
        }
        JkLog.endTask();
    }

    // https://www.programcreek.com/java-api-examples/index.php?api=javax.tools.DocumentationTool
    private void executeTool(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Files.createDirectories(outputDir);
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, JkUtilsIterable.listOf(outputDir.toFile()));
            Writer writer = new PrintWriter(new OutputStreamWriter(JkLog.getOutPrintStream(), StandardCharsets.UTF_8));
            List<String> options = computeOptions(classpath, srcDirs, outputDir);
            DocumentationTool.DocumentationTask task = tool.getTask(writer, fm, null, null,
                    options, null);
            task.call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void executeCommandLine(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        String exeName = JkUtilsSystem.IS_WINDOWS ? "javadoc.exe" : "javadoc";
        Path javadocExe = JkUtilsJdk.javaHome().resolve("bin/" + exeName);
        if (!Files.exists(javadocExe)) {
            javadocExe = JkUtilsJdk.javaHome().resolve("../bin/" + exeName).normalize();
        }
        boolean verbose = JkUtilsObject.firstNonNull(displayOutput, JkLog.isVerbose());
        JkLog.trace(javadocExe.toString());
        LinkedHashSet packages = computePackages(srcDirs);
        if (packages.isEmpty()) {
            JkLog.warn("No package detected. Skip Javadoc.");
            return;
        }
        JkProcess process = JkProcess.of(javadocExe.toString())
                .addParams(computeOptions(classpath, srcDirs, outputDir))
                .addParams(computePackages(srcDirs))
                .setLogOutput(verbose)
                .setLogCommand(verbose)
                .setFailOnError(true);
        try {
            process.exec();
        } catch (IllegalStateException e) {
            JkLog.warn("An error occurred when generating Javadoc. Maybe there is no public class to document." +
                    " Please relaunch the process with -LV option to see details");
        }
    }

    private LinkedHashSet<String> computePackages(JkPathTreeSet srcDirs) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Path relFile: srcDirs.getRelativeFiles()) {
            Path packageDir = relFile.getParent();
            if (packageDir != null) {
                String packageName = packageDir.toString().replace(File.separator, ".");
                result.add(packageName);
            }
        }
        return result;
    }

    private List<String> computeOptions(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        List<String> options = new LinkedList<>();
        if (!containsLike("-Xdoclint")) {
            options.add("-Xdoclint:none");
        }
        if (!contains("-sourcepath")) {
            options.add("-sourcepath");
            options.add(JkPathSequence.of(srcDirs.getRootDirsOrZipFiles()).toPath());
        }
        if (!contains("-d")) {
            options.add("-d");
            options.add(outputDir.toString());
        }
        if (!contains("-classpath") && classpath != null && classpath.iterator().hasNext()) {
            options.add("-classpath");
            options.add(JkPathSequence.of(classpath).toPath());
        }
        if (JkLog.isVerbose()) {
            options.add("-verbose");
        } else {
            options.add("-quiet");
        }
        options.addAll(this.options);
        JkLog.trace(options.toString());
        return options;
    }

    private boolean containsLike(String hint) {
        for (String option : options) {
            if (option.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String hint) {
        for (String option : options) {
            if (option.equals(hint)) {
                return true;
            }
        }
        return false;
    }

}
