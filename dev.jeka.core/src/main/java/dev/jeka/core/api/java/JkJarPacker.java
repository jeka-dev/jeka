package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Utilities class to produce Jar files.
 */
public final class JkJarPacker {

    /**
     * Filter to exclude signature files from fat jar (aka uber jar).
     */
    public static final JkPathMatcher EXCLUDE_SIGNATURE_MATCHER =
            JkPathMatcher.of(false, "meta-inf/*.rsa", "meta-inf/*.dsa", "meta-inf/*.sf");

    private final JkPathTreeSet classtrees;

    private final JkManifest manifest;

    private final JkPathTreeSet extraFiles;

    private JkJarPacker(JkPathTreeSet classTrees, JkManifest manifest, JkPathTreeSet extraFiles) {
        this.classtrees = classTrees;
        this.manifest = manifest;
        this.extraFiles = extraFiles;
    }

    public static JkJarPacker of(JkPathTreeSet classTrees) {
        JkUtilsAssert.argument(!classTrees.toList().isEmpty(), "Nothing to create jar from : " + classTrees);
        return new JkJarPacker(classTrees, null, null);
    }

    public static JkJarPacker of(Path classDir) {
        return of(JkPathTreeSet.ofRoots(classDir));
    }

    public JkJarPacker withManifest(JkManifest manifest) {
        return new JkJarPacker(this.classtrees, manifest, this.extraFiles);
    }

    public JkJarPacker withExtraFiles(JkPathTreeSet extraFiles) {
        return new JkJarPacker(this.classtrees, this.manifest, extraFiles);
    }

    /**
     * Creates a jar file including extra files and manifest.
     * @param resultFile Result file
     */
    public void makeJar(Path resultFile) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classtrees.toList().get(0).getRoot());
        }
        JkPathTreeSet treeSet = extraFiles == null ? JkPathTreeSet.ofEmpty() : extraFiles;
        classtrees.and(treeSet).zipTo(resultFile);
    }

    /**
     * Creates a fat jar file according specified parameters. Fat jar file including the content of other jars.
     * The result jar does not contain other jars as zip entry but content of the other jars is merged with the content
     * of original jar.
     * @param resultFile Result file
     * @param otherJars content of other jar to merge with the original jar
     * @param filter Only files matching this filter will be included in the resulting fat jar, either it comes from
     *               dependencies or not.
     */
    public void makeFatJar(Path resultFile, Iterable<Path> otherJars, PathMatcher filter) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classtrees.toList().get(0).getRoot());
        }
        JkPathTreeSet.ofEmpty().andZips(otherJars).and(classtrees).andMatcher(EXCLUDE_SIGNATURE_MATCHER)
                .andMatcher(filter)
                .zipTo(resultFile)  // main jar files must take precedence over files coming form dependencies
                .close();
    }


}
