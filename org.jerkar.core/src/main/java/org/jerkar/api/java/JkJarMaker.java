package org.jerkar.api.java;

import java.io.File;

import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.file.JkZipper;

/**
 * Utilities class to produce Jar files with conventional naming
 */
public final class JkJarMaker {

    /**
     * Filter to exclude signature files from fat jar (aka uber jar).
     */
    public static final JkPathFilter EXCLUDE_SIGNATURE_FILTER =
            JkPathFilter.exclude("meta-inf/*.rsa", "meta-inf/*.dsa", "meta-inf/*.sf").caseSensitive(false);

    private JkJarMaker() {
        // Not instantiable
    }

    /**
     * Creates a jar file according specified parameters.
     * @param resultFile Result file
     * @param classDir The folder containing elements to zip.
     * @param manifest Can be <code>null</code>.
     * @param extraFiles Extra files to embed in jar. Can be empty or <code>null</code>.
     */
    public static void jar(File resultFile, JkManifest manifest, File classDir, JkFileTreeSet extraFiles) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classDir);
        }
        JkFileTreeSet treeSet = extraFiles == null ? JkFileTreeSet.empty() : extraFiles;
        JkFileTreeSet.of(classDir).and(treeSet).zip().to(resultFile);
    }

    /**
     * Creates a fat jar file according specified parameters. Fat jar file including the content of other jars.
     * The result jar does not contains other jars as zip entry but content of the other jars is merged with the content
     * of original jar.
     * @param resultFile Result file
     * @param classDir The folder containing elements to zip.
     * @param manifest Can be <code>null</code>.
     * @param extraFiles Extra files to embed in jar. Can be empty or <code>null</code>.
     * @param otherJars content of other jar to merge with the original jar
     */
    public static void fatJar(File resultFile, JkManifest manifest, File classDir,
                              JkFileTreeSet extraFiles, Iterable<File> otherJars) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classDir);
        }
        JkFileTreeSet.of(classDir).and(extraFiles).zip().merge(otherJars).to(resultFile, EXCLUDE_SIGNATURE_FILTER);
    }

    /**
     * Creates a fat jar file according specified parameters. Fat jar file including the content of other jars.
     * The result jar does not contains other jars as zip entry but content of the other jars is merged with the content
     * of original jar.
     * @param originalJar the original Jar to create a fat jar from
     * @param otherJars content of other jar to merge with the original jar
     */
    public static void fatJar(File resultFile, File originalJar, Iterable<File> otherJars) {
        JkZipper.of(originalJar).merge(otherJars).to(resultFile, EXCLUDE_SIGNATURE_FILTER);
    }



}
