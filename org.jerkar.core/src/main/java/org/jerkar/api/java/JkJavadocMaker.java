package org.jerkar.api.java;


import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsJdk;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * Offers fluent interface for producing Javadoc.
 *
 * @author Jerome Angibaud
 */
public final class JkJavadocMaker {

    private static final String JAVADOC_MAIN_CLASS_NAME = "com.sun.tools.javadoc.Main";

    private final JkPathTreeSet srcDirs;

    private final List<String> extraArgs;

    private final Class<?> doclet;

    private final Iterable<Path> classpath;

    private final Path outputDir;

    private final Path zipFile;

    private JkJavadocMaker(JkPathTreeSet srcDirs, Class<?> doclet, Iterable<Path> classpath,
                           List<String> extraArgs, Path outputDir, Path zipFile) {
        this.srcDirs = srcDirs;
        this.extraArgs = extraArgs;
        this.doclet = doclet;
        this.classpath = classpath;
        this.outputDir = outputDir;
        this.zipFile = zipFile;
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory then compacted in the specified zip file.
     */
    public static JkJavadocMaker of(JkPathTreeSet sources, Path outputDir, Path zipFile) {
        return new JkJavadocMaker(sources, null, null, new LinkedList<>(), outputDir, zipFile);
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory.
     */
    public static JkJavadocMaker of(JkPathTreeSet sources, Path outputDir) {
        return new JkJavadocMaker(sources, null, null, new LinkedList<>(), outputDir, null);
    }


    /**
     * Returns the zip file containing all the produced Javadoc.
     */
    public Path zipFile() {
        return zipFile;
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified doclet.
     */
    public JkJavadocMaker withDoclet(Class<?> doclet) {
        if (doclet == null) {
            return this;
        }
        return new JkJavadocMaker(srcDirs, doclet, classpath, extraArgs, outputDir, zipFile);
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
        return new JkJavadocMaker(srcDirs, doclet, classpath, list, outputDir, zipFile);
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified classpath.
     */
    public JkJavadocMaker withClasspath(Iterable<Path> classpath) {
        return new JkJavadocMaker(srcDirs, doclet, JkUtilsPath.disambiguate(classpath), extraArgs, outputDir, zipFile);
    }

    /**
     * Actually processes and creates the javadoc files.
     */
    public void process() {
        JkEvent.start(this,"Generating javadoc");
        if (this.srcDirs.hasNoExistingRoot()) {
            JkEvent.warn(this,"No sources found in " + this.srcDirs);
            JkEvent.end(this, "");
            return;
        }
        final String[] args = toArguments(outputDir);
        final OutputStream warn;
        final OutputStream error;
        if (JkEvent.verbosity() == JkEvent.Verbosity.VERBOSE) {
            warn = JkEvent.stream();
            error = JkEvent.errorStream();
        } else {
            warn = JkUtilsIO.nopPrintStream();
            error = JkUtilsIO.nopPrintStream();
        }
        JkUtilsPath.createDirectories(outputDir);
        execute(doclet, JkEvent.stream(), warn, error, args);
        if (Files.exists(outputDir) && zipFile != null) {
            JkPathTree.of(outputDir).zipTo(zipFile);
        }
        JkEvent.end(this, "");
    }

    private String[] toArguments(Path outputDir) {
        final List<String> list = new LinkedList<>();
        list.add("-sourcepath");
        list.add(JkPathSequence.ofMany(this.srcDirs.rootDirsOrZipFiles()).toString());
        list.add("-d");
        list.add(outputDir.toAbsolutePath().toString());
        if (JkEvent.verbosity() == JkEvent.Verbosity.VERBOSE) {
            list.add("-verbose");
        } else {
            list.add("-quiet");
        }
        list.add("-docletpath");
        list.add(JkUtilsJdk.toolsJar().toString());
        if (classpath != null && classpath.iterator().hasNext()) {
            list.add("-classpath");
            list.add(JkPathSequence.ofMany(this.classpath).toString());
        }
        list.addAll(extraArgs);

        for (final Path sourceFile : this.srcDirs.files()) {
            if (sourceFile.getFileName().toString().endsWith(".java")) {
                list.add(sourceFile.toString());
            }

        }
        return list.toArray(new String[0]);
    }

    private static void execute(Class<?> doclet, OutputStream normalStream, OutputStream warnStream,
                                OutputStream errorStream, String[] args) {

        final String docletString = doclet != null ? doclet.getName()
                : "com.sun.tools.doclets.standard.Standard";
        final Class<?> mainClass = getJavadocMainClass();
        JkUtilsReflect.newInstance(mainClass);
        final Method method = JkUtilsReflect.getMethod(mainClass, "execute", String.class,
                PrintWriter.class, PrintWriter.class, PrintWriter.class, String.class,
                String[].class);
        JkUtilsReflect.invoke(null, method, "Javadoc", new PrintWriter(errorStream),
                new PrintWriter(warnStream), new PrintWriter(normalStream), docletString, args);
    }

    private static Class<?> getJavadocMainClass() {
        final JkClassLoader classLoader = JkClassLoader.current();
        Class<?> mainClass = classLoader.loadIfExist(JAVADOC_MAIN_CLASS_NAME);
        if (mainClass == null) {
            classLoader.addEntry(JkUtilsJdk.toolsJar());
            mainClass = classLoader.loadIfExist(JAVADOC_MAIN_CLASS_NAME);
            if (mainClass == null) {
                throw new RuntimeException(
                        "It seems that you are running a JRE instead of a JDK, please run Jerkar using a JDK.");
            }
        }
        return mainClass;
    }

}
