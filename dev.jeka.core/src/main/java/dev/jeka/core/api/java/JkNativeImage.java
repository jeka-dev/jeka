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

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JkNativeImage {

    public enum StaticLinkage {
        NONE, FULLY, MOSTLY
    }

    public final ReachabilityMetadata reachabilityMetadata = new ReachabilityMetadata();

    private final Path fatJar;

    private final List<Path> classpath;

    private StaticLinkage staticLinkage = StaticLinkage.NONE;

    private JkNativeImage(Path fatJar, List<Path> classpath) {
        this.fatJar = fatJar;
        this.classpath = classpath;
    }

    public static JkNativeImage ofFatjar(Path fatJar) {
        return new JkNativeImage(fatJar, null);
    }

    public static JkNativeImage ofClasspath(List<Path> classpath) {
        return new JkNativeImage(null, classpath);
    }

    public JkNativeImage setStaticLinkage(StaticLinkage staticLinkage) {
        this.staticLinkage = staticLinkage;
        return this;
    }

    /**
     * Generates a native image at the specified location
     * @param target
     */
    public void make(Path target) {
        JkLog.startTask("make-native-image");
        assertToolPresent();
        boolean hasMessageBundle = false;
        final Path resourceBundle;
        if (fatJar != null) {
            try (JkZipTree jarTree = JkZipTree.of(fatJar)) {
                resourceBundle = jarTree.get("MessagesBundle.properties");
            }
        } else if (!classpath.isEmpty()) {
            Path mainDir = classpath.get(0);
            JkPathTree pathTree = JkPathTree.of(mainDir);
            resourceBundle = pathTree.get("MessagesBundle.properties");
        } else {
            resourceBundle = null;
        }
        if (resourceBundle != null && Files.exists(resourceBundle)) {
            hasMessageBundle = true;
            JkLog.verbose("Found MessagesBundle to include in the native image.");
        }

        String nativeImageExe = toolPath().toString();

        String regexp = "^(?!.*\\.class$).*$";
        //String regexp = ".*(?<!\\.class)"; // works on linux/macos only
        JkProcess process = JkProcess.of(nativeImageExe);
        if (classpath == null) {
            process = process.addParams("-jar", fatJar.toString());

            // For jar we need to explicitly declare resources and message bundles
            process
                    .addParams("-H:IncludeResources=" + regexp)
                    .addParamsIf(hasMessageBundle, "-H:IncludeResourceBundles=MessagesBundle");
        } else {
            String classpathContent = JkPathSequence.of(classpath).toPath();
            final String classpathArg;
            if (classpathContent.length() <= 2048) {
                classpathArg = classpathContent;
            } else {
                Path tempArgFile = JkUtilsPath.createTempFile("jeka-native-image-cp", ".txt");
                JkPathFile.of(tempArgFile).write(classpathContent);
                classpathArg = "@" + tempArgFile;
            }
            process = process.addParams("-classpath", classpathArg);
        }
        if (staticLinkage == StaticLinkage.FULLY) {
            process.addParams("--static", "--libc=musl");
        } else if (staticLinkage == StaticLinkage.MOSTLY) {
            process.addParams("--static-nolibc");
        }
        process = process
                .addParams("--no-fallback")
                .addParams("-o",  target.toString())
                .addParams("-H:+UnlockExperimentalVMOptions")
                .setLogCommand(true)
                .setCollectStderr(true)
                .setCollectStdout(true)
                .setLogWithJekaDecorator(true)
                .setDestroyAtJvmShutdown(true);
        String reachabilityArg = reachabilityMetadata.repoDirsToIncludeAsString();
        if (!reachabilityArg.isEmpty()) {
            process.addParams("-H:ConfigurationFileDirectories="+reachabilityArg);
        }
        process.exec();
        JkLog.info("Generated in %s", target);
        JkLog.endTask();
    }

    public Path make() {
        JkUtilsAssert.state(fatJar != null, "You must specify a target path when creating a native image without using" +
                " a JAR file. Use make(targetPath) instead.");
        String relTarget = JkUtilsString.substringBeforeLast(fatJar.toString(), ".jar");
        if (relTarget.endsWith("-fat")) {
            relTarget = JkUtilsString.substringBeforeLast(relTarget, "-fat");
        }
        Path target = Paths.get(relTarget);
        make(target);
        return Paths.get(relTarget);
    }

    private static Path toolPath() {
        Path javaHome = JkJavaProcess.CURRENT_JAVA_HOME;
        return JkUtilsSystem.IS_WINDOWS ?
                javaHome.resolve("bin/native-image.cmd") :
                javaHome.resolve("bin/native-image");
    }

    private static boolean isPresent() {
        try {
            return JkProcess.of(toolPath().toString(), "--version")
                    .exec()
                    .hasSucceed();
        } catch (UncheckedIOException e) {
            return false;
        }
    }

    private static void assertToolPresent() {
        if (!isPresent()) {
            throw new IllegalStateException("The project seems not to be configured for using graalvm JDK.\n" +
                    "Please set 'jeka.java.distrib=graalvm' in jeka.properties in order to build native images.");
        }
    }






    public class ReachabilityMetadata {

        private boolean useRepo = true;

        // The version of the metadata repo to download.
        private String repoVersion = "0.10.3";

        // repos for downloading reachability metadata
        private JkRepoSet downloadRepos = JkRepo.ofMavenCentral().toSet();

        private Path extractDir = Paths.get(JkConstants.OUTPUT_PATH + "/graalvm-reachability-metadata-repo");

        // Dependencies for which looking for reachability metadata.
        // This should contain all transitive dependencies used at runtime
        private Supplier<? extends Collection<JkCoordinate>> dependencySupplier;

        public ReachabilityMetadata setUseRepo(boolean useRepo) {
            this.useRepo = useRepo;
            return this;
        }

        public ReachabilityMetadata setRepoVersion(String repoVersion) {
            this.repoVersion = repoVersion;
            return this;
        }

        public ReachabilityMetadata setExtractDir(Path extractDir) {
            this.extractDir = extractDir;
            return this;
        }

        public ReachabilityMetadata setDependencies(Supplier<? extends Collection<JkCoordinate>> dependencySupplier) {
            this.dependencySupplier = dependencySupplier;
            return this;
        }

        public ReachabilityMetadata setDependencies(Collection<JkCoordinate> dependencies) {
            this.dependencySupplier = () -> dependencies;
            return this;
        }

        private String repoDirsToIncludeAsString() {
            List<String> pathStrings = repoDirsToInclude().stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());
            return String.join(",", pathStrings);
        }

        private List<Path> repoDirsToInclude() {
            if (!useRepo) {
                return Collections.emptyList();
            }
            extractReachabilityMetadata();
            Repo repo = new Repo(extractDir);
            if (dependencySupplier != null) {
                JkLog.verbose("Extracting reachability metadata from resolved dependencies");
                return dependencySupplier.get().stream()
                        .map(repo::metadataFolderFor)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (classpath != null) {
                JkLog.verbose("Extracting reachability metadata from classpath");
                return classpath.stream()
                        .map(repo::inferCoordinateFromPath)
                        .filter(Objects::nonNull)
                        .map(repo::metadataFolderFor)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                JkLog.warn("No dependencies were supplied, and no classpath was declared. " +
                        "Unable to find any reachability metadata..");
                return Collections.emptyList();
            }
        }

        private void extractReachabilityMetadata() {
            Path zip = reachabilityMetadataZipRepoPath();
            try (JkZipTree zipTree = JkZipTree.of(zip)) {
                zipTree.copyTo(extractDir, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        private Path reachabilityMetadataZipRepoPath() {
            return JkCoordinateFileProxy.of(downloadRepos, reachabilityMetadataZipRepo(repoVersion)).get();
        }

        private JkCoordinate reachabilityMetadataZipRepo(String version) {
            return JkCoordinate.of("org.graalvm.buildtools:graalvm-reachability-metadata:repository:zip:%s", version);
        }


        private class Repo {

            private final Path repoRoot;

            private Repo(Path repoRoot) {
                this.repoRoot = repoRoot;
            }

            Path metadataFolderFor(JkCoordinate coordinate) {
                String group = coordinate.getModuleId().getGroup();
                Path groupDir = repoRoot.resolve(group);
                if (!Files.exists(groupDir) || !Files.isDirectory(groupDir)) {
                    return null;
                }
                Path artifactDir = groupDir.resolve(coordinate.getModuleId().getName());
                if (!Files.exists(artifactDir) || !Files.isDirectory(artifactDir)) {
                    return null;
                }
                return findDirForVersion(artifactDir, coordinate);
            }

            private Path findDirForVersion(Path artifactDir, JkCoordinate coordinate) {
                List<JkVersion> versions;
                try (Stream<Path> allVersions = Files.list(artifactDir)){
                    versions = allVersions
                            .filter(Files::isDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .map(JkVersion::of)
                            .sorted()
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (versions.contains(JkVersion.of(coordinate.getVersion().getValue()))) {
                    return artifactDir.resolve(coordinate.getVersion().getValue());
                } else if(!versions.isEmpty()) {
                    JkVersion lastVersion = versions.get(versions.size() - 1);
                    JkLog.warn("Reachability metadata could not find version for %s. Use latest version %s instead.",
                            coordinate, lastVersion);
                    return artifactDir.resolve(lastVersion.getValue());
                } else {
                    JkLog.debug("No Reachability metadata could be found for %s", coordinate.getModuleId());
                    return null;
                }
            }

            private JkCoordinate inferCoordinateFromPath(Path jarFile) {
                String fileName = jarFile.getFileName().toString();
                String extensionLessName = JkUtilsString.substringBeforeLast(fileName, ".jar");
                if (extensionLessName.isEmpty()) {
                    return null;
                }
                String version = JkUtilsString.substringAfterLast(fileName, "-");
                if (version.isEmpty()) {
                    return null;
                }
                Path parent = jarFile.getParent();
                if (parent == null || !"jars".equals(parent.getFileName().toString())) {
                    return null;
                }
                Path artifactDir = parent.getParent();
                if (artifactDir == null) {
                    return null;
                }
                Path groupDir = artifactDir.getParent();
                if (groupDir == null) {
                    return null;
                }
                return JkCoordinate.of(
                        groupDir.getFileName().toString(),
                        artifactDir.getFileName().toString(),
                        version);
            }

        }
    }

}
