package org.jerkar.plugins.protobuf;

import java.io.File;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.utils.JkUtilsFile;

public class JkUtilsProtobuf {

    /**
     * Get .java files from the specified source directories and package.
     */
    public static JkFileTreeSet getJavaFiles(JkFileTreeSet sources, String packageName) {
        String packagePath = packageName.replace('.', '/');
        return sources.andFilter(JkPathFilter.include(packagePath + "/*.java"));
    }
    
    /**
     * Delete all .java files from the specified source directories and package.
     */
    public static void deleteJavaFiles(JkFileTreeSet sources, String packageName) {
        deleteFiles(getJavaFiles(sources, packageName));
    }
    
    private static void deleteFiles(JkFileTreeSet files) {
        for (JkFileTree tree : files.fileTrees()) {
            if (tree.exists()) {
                tree.deleteAll();
            }
        }
    }
    
    /**
     * Add SuppressWarnings annotations to all the .java files in the source directories and package.
     */
    public static void suppressWarnings(JkFileTreeSet sources, String packageName) {
        suppressWarnings(getJavaFiles(sources, packageName));
    }
    
    /**
     * Add SuppressWarnings annotations to the specified files.
     */
    public static void suppressWarnings(JkFileTree javaFiles) {
        suppressWarnings(JkFileTreeSet.of(javaFiles));
    }

    /**
     * Add SuppressWarnings annotations to the specified files.
     */
    public static void suppressWarnings(JkFileTreeSet javaFiles) {
        for (File javaFile : javaFiles) {
            suppressWarnings(javaFile);
        }
    }

    /**
     * Add a SuppressWarnings annotation to the specified file.
     */
    public static void suppressWarnings(File javaFile) {
        String source = JkUtilsFile.read(javaFile);
        source = source.replace("public final class", "@SuppressWarnings(\"all\")\npublic final class");
        JkUtilsFile.writeString(javaFile, source, false);
    }
}
