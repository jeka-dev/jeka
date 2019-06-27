package dev.jeka.core.api.java;


import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import javax.tools.DocumentationTool;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Offers fluent interface for producing Javadoc.
 *
 * @author Jerome Angibaud
 */
public final class JkJavadocMaker {

    private static final String JAVADOC_MAIN_CLASS_NAME = "com.sun.tools.javadoc.Main";

    private final JkPathTreeSet srcDirs;

    private final List<String> extraArgs;

    private final Iterable<Path> classpath;

    private final Path outputDir;

    private final Path zipFile;

    private JkJavadocMaker(JkPathTreeSet srcDirs, Iterable<Path> classpath,
                           List<String> extraArgs, Path outputDir, Path zipFile) {
        this.srcDirs = srcDirs;
        this.extraArgs = extraArgs;
        this.classpath = classpath;
        this.outputDir = outputDir;
        this.zipFile = zipFile;
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory then compacted in the specified zip file.
     */
    public static JkJavadocMaker of(JkPathTreeSet sources, Path outputDir, Path zipFile) {
        return new JkJavadocMaker(sources, null,  new LinkedList<>(), outputDir, zipFile);
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory.
     */
    public static JkJavadocMaker of(JkPathTreeSet sources, Path outputDir) {
        return new JkJavadocMaker(sources, null,  new LinkedList<>(), outputDir, null);
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
        return new JkJavadocMaker(srcDirs, classpath, list, outputDir, zipFile);
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified classpath.
     */
    public JkJavadocMaker withClasspath(Iterable<Path> classpath) {
        return new JkJavadocMaker(srcDirs, JkUtilsPath.disambiguate(classpath), extraArgs, outputDir, zipFile);
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
        executeTool(outputDir, srcDirs, classpath);
        if (Files.exists(outputDir) && zipFile != null) {
            JkPathTree.of(outputDir).zipTo(zipFile);
        }
        JkLog.endTask();
    }

    // https://www.programcreek.com/java-api-examples/index.php?api=javax.tools.DocumentationTool
    private static void executeTool(Path outputDir, JkPathTreeSet sourceTreeSet, Iterable<Path> classpath) {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Files.createDirectories(outputDir);
            ///fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, JkUtilsIterable.listOf(outputDir.toFile()));
            Writer writer = new PrintWriter(new OutputStreamWriter(JkLog.getOutputStream(), "UTF-8"));
            List<String> options = new LinkedList<>();
            options.add("-sourcepath");
            options.add(JkPathSequence.of(sourceTreeSet.getRootDirsOrZipFiles()).toString());
            options.add("-subpackages");
            options.add(".");
            options.add("-d");
            options.add(outputDir.toString());
            if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
                options.add("-verbose");
            } else {
                ///options.add("-quiet");
            }
            if (classpath != null && classpath.iterator().hasNext()) {
                options.add("-classpath");
                options.add(JkPathSequence.of(classpath).toString());
            }
            DocumentationTool.DocumentationTask task = tool.getTask(writer, null, null, null,
                    options, null);
            options.add("-verbose");
            ///options.add("*");
            task.call();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
