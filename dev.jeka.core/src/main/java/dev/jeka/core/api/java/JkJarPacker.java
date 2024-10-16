/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.jar.JarFile;

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

    public static JkJarPacker of(JkPathTree classTree) {
        return of(classTree.toSet());
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
     * @param extraEntries List of other jars or class dirs to merge with the original jar
     * @param filter Only files matching this filter will be included in the resulting fat jar, either it comes from
     *               dependencies or not.
     */
    public void makeFatJar(Path resultFile, Iterable<Path> extraEntries, PathMatcher filter) {
        Path originalJar = JkUtilsPath.createTempFile("jk-jar-original", ".jar");
        JkUtilsPath.deleteFile(originalJar);
        classtrees.andMatcher(filter).andMatcher(EXCLUDE_SIGNATURE_MATCHER).zipTo(originalJar);
        JkJarWriter jarWriter = JkJarWriter.of(resultFile);
        jarWriter.writeEntries(JkUtilsZip.jarFile(originalJar));
        if (manifest != null && !manifest.isEmpty()) {
            jarWriter.writeManifest(manifest.getManifest());
        }
        for (Path extraEntry : extraEntries) {
            if (Files.isDirectory(extraEntry)) {
                jarWriter.writeEntries(extraEntry, EXCLUDE_SIGNATURE_MATCHER);
            } else {
                try (JarFile jarFile = JkUtilsZip.jarFile(extraEntry)) {
                    jarWriter.writeEntries(jarFile);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        jarWriter.close();

        // On Windows, this may happen that the deletion fails cause another process is using it
        JkUtilsPath.deleteIfExistsSafely(originalJar);
    }

    public static void makeFatJar(Iterable<Path> jarOrDirs, Path outputJar) {
        JkJarWriter jarWriter = JkJarWriter.of(outputJar);
        for (Path entry : jarOrDirs) {
            if (Files.isDirectory(entry)) {
                jarWriter.writeEntries(entry, EXCLUDE_SIGNATURE_MATCHER);
            } else {
                try (JarFile jarFile = JkUtilsZip.jarFile(entry)) {
                    jarWriter.writeEntries(jarFile);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        jarWriter.close();
    }

    public static void makeShadeJar(Path originalJar, Iterable<Path> extraJars, Path outputJar) {
        JkInternalJarShader.of(JkProperties.ofSysPropsThenEnvThenGlobalProperties())
                .shade(originalJar, JkUtilsIterable.setOf(extraJars), outputJar);
    }

}
