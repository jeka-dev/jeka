package org.jerkar.api.java;

import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.file.JkPathMatcher;

import java.nio.file.Path;

/**
 * Utilities class to produce Jar files with conventional naming
 */
public final class JkJarMaker {

    /**
     * Filter to exclude signature files from fat jar (aka uber jar).
     */
    public static final JkPathMatcher EXCLUDE_SIGNATURE_MATCHER =
            JkPathMatcher.ofReject("meta-inf/*.rsa", "meta-inf/*.dsa", "meta-inf/*.sf");

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
    public static void jar(Path resultFile, JkManifest manifest, Path classDir, JkPathTreeSet extraFiles) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classDir);
        }
        JkPathTreeSet treeSet = extraFiles == null ? JkPathTreeSet.ofEmpty() : extraFiles;
        JkPathTreeSet.of(classDir).and(treeSet).zipTo(resultFile);
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
    public static void fatJar(Path resultFile, JkManifest manifest, Path classDir,
                              JkPathTreeSet extraFiles, Iterable<Path> otherJars) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classDir);
        }

        JkPathTreeSet.of(classDir).and(extraFiles).andZips(otherJars)
                .andMatcher(EXCLUDE_SIGNATURE_MATCHER).zipTo(resultFile);

    }

}
