package org.jerkar.api.java;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsJdk;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * Offers fluent interface for producing Javadoc.
 * 
 * @author Jerome Angibaud
 */
public final class JkJavadocMaker {

    private static final String JAVADOC_MAIN_CLASS_NAME = "com.sun.tools.javadoc.Main";

    private final JkFileTreeSet srcDirs;

    private final String extraArgs;

    private final Class<?> doclet;

    private final Iterable<File> classpath;

    private final File outputDir;

    private final File zipFile;

    private JkJavadocMaker(JkFileTreeSet srcDirs, Class<?> doclet, Iterable<File> classpath,
            String extraArgs, File outputDir, File zipFile) {
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
    public static JkJavadocMaker of(JkFileTreeSet sources, File outputDir, File zipFile) {
        return new JkJavadocMaker(sources, null, null, "", outputDir, zipFile);
    }

    /**
     * Creates a {@link JkJavadocMaker} from the specified sources. The result will be outputed in
     * the specified directory.
     */
    public static JkJavadocMaker of(JkFileTreeSet sources, File outputDir) {
        return new JkJavadocMaker(sources, null, null, "", outputDir, null);
    }

    /**
     * Returns the zip file containing all the produced Javadoc.
     */
    public File zipFile() {
        return zipFile;
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified doclet.
     */
    public JkJavadocMaker withDoclet(Class<?> doclet) {
        return new JkJavadocMaker(srcDirs, doclet, classpath, extraArgs, outputDir, zipFile);
    }

    /**
     * Returns a {@link JkJavadocMaker} identical to this one but using the specified classpath.
     */
    public JkJavadocMaker withClasspath(Iterable<File> classpath) {
        return new JkJavadocMaker(srcDirs, doclet, classpath, extraArgs, outputDir, zipFile);
    }

    /**
     * Actually processes and creates the javadoc files.
     */
    public void process() {
        JkLog.startln("Generating javadoc");
        final String[] args = toArguments(outputDir);
        final PrintStream warn;
        final PrintStream error;
        if (JkLog.verbose()) {
            warn = JkLog.warnStream();
            error = JkLog.errorStream();
        } else {
            warn = JkUtilsIO.nopPrintStream();
            error = JkUtilsIO.nopPrintStream();
        }
        execute(doclet, JkLog.infoStream(), warn, error, args);
        if (outputDir.exists() && zipFile != null) {
            JkFileTree.of(outputDir).zip().to(zipFile);
        }
        JkLog.done();
    }

    private String[] toArguments(File outputDir) {
        final List<String> list = new LinkedList<String>();
        list.add("-sourcepath");
        list.add(JkPath.of(this.srcDirs.roots()).toString());
        list.add("-d");
        list.add(outputDir.getAbsolutePath());
        if (JkLog.verbose()) {
            list.add("-verbose");
        } else {
            list.add("-quiet");
        }
        list.add("-docletpath");
        list.add(JkUtilsJdk.toolsJar().getPath());
        if (classpath != null && classpath.iterator().hasNext()) {
            list.add("-classpath");
            list.add(JkPath.of(this.classpath).toString());
        }
        if (!this.extraArgs.trim().isEmpty()) {
            final String[] extraArgs = this.extraArgs.split(" ");
            list.addAll(Arrays.asList(extraArgs));
        }

        for (final File sourceFile : this.srcDirs.files(false)) {
            if (sourceFile.getPath().endsWith(".java")) {
                list.add(sourceFile.getAbsolutePath());
            }

        }
        return list.toArray(new String[0]);
    }

    private static void execute(Class<?> doclet, PrintStream normalStream, PrintStream warnStream,
            PrintStream errorStream, String[] args) {

        final String docletString = doclet != null ? doclet.getName()
                : "com.sun.tools.doclets.standard.Standard";
        final Class<?> mainClass = getJavadocMainClass();
        JkUtilsReflect.newInstance(mainClass);
        final Method method = JkUtilsReflect.getMethod(mainClass, "execute", String.class,
                PrintWriter.class, PrintWriter.class, PrintWriter.class, String.class,
                new String[0].getClass());
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
