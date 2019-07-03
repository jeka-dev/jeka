package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import javax.tools.DocumentationTool;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Provides fluent interface for producing Javadoc.
 *
 * @author Jerome Angibaud
 */
public final class JkJavadocMaker {

    private final JkPathTreeSet srcDirs;

    private final List<String> extraArgs;

    private final Iterable<Path> classpath;

    private final Path outputDir;

    private final Path zipFile;

    private final boolean displayOutput;

    private JkJavadocMaker(JkPathTreeSet srcDirs, Iterable<Path> classpath,
                           List<String> extraArgs, Path outputDir, Path zipFile, boolean displayOutput) {
        this.srcDirs = srcDirs;
        this.extraArgs = extraArgs;
        this.classpath = classpath;
        this.outputDir = outputDir;
        this.zipFile = zipFile;
        this.displayOutput = displayOutput;
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory then compacted in the specified zip file.
     */
    public static JkJavadocMaker of(JkPathTreeSet sources, Path outputDir, Path zipFile) {
        return new JkJavadocMaker(sources, Collections.emptyList(),  new LinkedList<>(), outputDir, zipFile,
                JkLog.isVerbose());
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory.
     */
    public static JkJavadocMaker of(JkPathTreeSet sources, Path outputDir) {
        return new JkJavadocMaker(sources, Collections.emptyList(),  new LinkedList<>(), outputDir, null,
                JkLog.isVerbose());
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified options (-classpath , -exclude, -subpackages, ...).
     */
    public JkJavadocMaker andOptions(String ... options) {
        return andOptions(Arrays.asList(options));
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified options (-classpath , -exclude, -subpackages, ...).
     */
    public JkJavadocMaker andOptions(List<String> options) {
        final List<String> list = new LinkedList<>(this.extraArgs);
        list.addAll(options);
        return new JkJavadocMaker(srcDirs, classpath, list, outputDir, zipFile, displayOutput);
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified classpath.
     */
    public JkJavadocMaker withClasspath(Iterable<Path> classpath) {
        return new JkJavadocMaker(srcDirs, JkUtilsPath.disambiguate(classpath), extraArgs, outputDir, zipFile,
                displayOutput);
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified classpath.
     */
    public JkJavadocMaker withDisplayOutput(boolean displayOutput) {
        return new JkJavadocMaker(srcDirs, JkUtilsPath.disambiguate(classpath), extraArgs, outputDir, zipFile,
                displayOutput);
    }

    /**
     * Actually processes and creates the javadoc files.
     */
    public void process() {
        JkLog.startTask("Generating javadoc");
        if (this.srcDirs.hasNoExistingRoot()) {
            JkLog.warn("No sources found in " + this.srcDirs);
            return;
        }
        //executeTool();
        executeCommandLine();
        if (Files.exists(outputDir) && zipFile != null) {
            JkPathTree.of(outputDir).zipTo(zipFile);
        }
        JkLog.endTask();
    }

    // https://www.programcreek.com/java-api-examples/index.php?api=javax.tools.DocumentationTool
    private void executeTool() {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Files.createDirectories(outputDir);
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, JkUtilsIterable.listOf(outputDir.toFile()));
            Writer writer = new PrintWriter(new OutputStreamWriter(JkLog.getOutputStream(), "UTF-8"));
            List<String> options = computeOptions();
            DocumentationTool.DocumentationTask task = tool.getTask(writer, fm, null, null,
                    options, null);
            task.call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void executeCommandLine() {
        String exeName = JkUtilsSystem.IS_WINDOWS ? "javadoc.exe" : "javadoc";
        Path javadocExe = JkUtilsJdk.javaHome().resolve("bin/" + exeName);
        if (!Files.exists(javadocExe)) {
            javadocExe = JkUtilsJdk.javaHome().resolve("../bin/" + exeName).normalize();
        }
        JkLog.trace(javadocExe.toString());
        JkProcess.of(javadocExe.toString())
                .andParams(computeOptions())
                .withLogOutput(displayOutput)
                .withFailOnError(true)
                .runSync();
    }

    private List<String> computeOptions() {
        List<String> options = new LinkedList<>();
        if (!containsLike("-Xdoclint")) {
            options.add("-Xdoclint:none");
        }
        if (!contains("-sourcepath")) {
            options.add("-sourcepath");
            options.add(JkPathSequence.of(srcDirs.getRootDirsOrZipFiles()).toString());
        }
        if (!contains("-subpackages")) {
            options.add("-subpackages");
            options.add(computeSubpackages());
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
        options.addAll(this.extraArgs);
        JkLog.trace(options.toString());
        return options;
    }

    private String computeSubpackages() {
        List<String> dirs = new LinkedList<>();
        for (Path root : this.srcDirs.getRootDirsOrZipFiles()) {
            try (Stream<Path> pathStream = Files.list(root).filter(path ->Files.isDirectory(path))) {
                pathStream.forEach(path -> {
                    JkPathTree pathTree = JkPathTree.of(path).andMatching("*.java", "**/*.java");
                    if (pathTree.count(0, false) > 0) {
                        dirs.add(path.getFileName().toString());
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return String.join(":", dirs);
    }

    private boolean containsLike(String hint) {
        for (String option : extraArgs) {
            if (option.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String hint) {
        for (String option : extraArgs) {
            if (option.equals(hint)) {
                return true;
            }
        }
        return false;
    }

}
