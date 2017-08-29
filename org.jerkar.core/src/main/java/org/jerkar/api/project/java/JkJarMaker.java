package org.jerkar.api.project.java;

import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkManifest;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Deprecated // Experimental
public class JkJarMaker {

    public static JkJarMaker of(File outputDir, String baseName) {
        return new JkJarMaker(outputDir, new HashSet<String>(), baseName);
    }

    private final File outputDir;

    private final Set<String> checkSums;

    private final String baseName;

    private JkJarMaker(File outputDir, Set<String> checkSums, String baseName) {
        this.outputDir = outputDir;
        this.checkSums = checkSums;
        this.baseName = baseName;
    }

    public File mainJar(JkManifest manifest, File classDir, JkFileTreeSet extraFiles) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classDir);
        }
        File jarFile = jarFile(null);
        JkFileTreeSet.of(classDir).and(extraFiles)
                .zip().to(jarFile)
                .md5If(checkSums.contains("MD5"))
                .sha1If(checkSums.contains("SHA-1"));
        return jarFile;
    }

    public File testJar(File classDir) {
        File jarFile = jarFile("test");
        JkFileTreeSet.of(classDir)
                .zip().to(jarFile)
                .md5If(checkSums.contains("MD5"))
                .sha1If(checkSums.contains("SHA-1"));
        return jarFile;
    }

    public File fatJar(JkManifest manifest, File classDir, JkClasspath embeddedJars, JkFileTreeSet extraFiles) {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(classDir);
        }
        File jarFile = jarFile("fat");
        JkFileTreeSet.of(classDir).and(extraFiles)
                .zip().merge(embeddedJars)
                .to(jarFile).md5If(checkSums.contains("MD5"))
                .sha1If(checkSums.contains("SHA-1"));
        return jarFile;
    }

    public File jar(JkFileTreeSet fileTreeSet, String suffix) {
        File file = jarFile(suffix);
        fileTreeSet.zip().to(file);
        return file;
    }

    public File jarFile(String classifier) {
        return file(classifier, "jar");
    }

    public File file(String classifier, String extension) {
        String extensionString = extension == null ? "" : "." + extension;
        if (classifier == null) {
            return new File(outputDir, baseName + extensionString);
        }
        return new File(outputDir, baseName + "-" + classifier + extensionString);
    }

}
