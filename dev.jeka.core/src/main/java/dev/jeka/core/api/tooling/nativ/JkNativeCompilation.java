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

package dev.jeka.core.api.tooling.nativ;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles native compilation to produce executables from a list of JAR files.
 * <p>
 * This class allows users to provide a list of JAR files and optional parameters
 * to generate a native executable.
 * </p>
 *
 * <h3>Native Compilation Flow</h3>
 * <ol>
 *   <li>Fetch the AOT (Ahead Of Time) metadata repository, which contains supplementary information for the compiler.</li>
 *   <li>Construct the command-line arguments required for the GraalVM <code>nativeImage</code> program.</li>
 *   <li>Download GraalVM if it is not already available on the system.</li>
 *   <li>Invoke the GraalVM <code>nativeImage</code> program to produce the executable from the JAR files.</li>
 * </ol>
 *
 * <h3>Configuration Options</h3>
 * <ul>
 *   <li>Specify whether to statically link the libc library.</li>
 *   <li>Define the main class to be executed in the generated executable.</li>
 *   <li>Decide whether to include all resources within the executable.</li>
 *   <li>Provide raw parameters to the <i>GraalVM nativeImage</i> for advanced configuration.</li>
 * </ul>
 *
 * <p>
 * Once configured, users can execute the <code>make</code> method to initiate the compilation process.
 * </p>
 */
public class JkNativeCompilation {

    public static final String DEFAULT_GRAALVM_VERSION = "23";;

    public static final String DEFAULT_REPO_VERSION =  "0.10.3";

    public enum StaticLink {

        /** No static linkage **/
        NONE,

        /** Static-link everything except  **/
        MOSTLY,

        /** Static-link everything using musl-libc **/
        MUSL,
    }

    public final ReachabilityMetadata reachabilityMetadata = new ReachabilityMetadata();

    private final List<Path> classpath;

    private StaticLink staticLink = StaticLink.NONE;

    private String mainClass;

    private final List<String> extraParams = new LinkedList<>();

    private boolean includesAllResources = false;

    private JkNativeCompilation(List<Path> classpath) {
        this.classpath = classpath;
    }

    public static JkNativeCompilation ofClasspath(List<Path> classpath) {
        return new JkNativeCompilation(classpath);
    }

    public JkNativeCompilation setStaticLinkage(StaticLink staticLink) {
        this.staticLink = staticLink;
        return this;
    }

    public JkNativeCompilation addExtraParams(String... params) {
        this.extraParams.addAll(Arrays.asList(params));
        return this;
    }

    public JkNativeCompilation setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public JkNativeCompilation setIncludesAllResources(boolean includesAllResources) {
        this.includesAllResources = includesAllResources;
        return this;
    }

    public List<Path> getClasspath() {
        return classpath;
    }

    public StaticLink getStaticLinkage() {
        return staticLink;
    }

    /**
     * Generates a native image at the specified file location.
     */
    public void make(Path outputFile) {
        JkLog.startTask("compile-native-executable");

        String nativeImageExe = toolPath().toString();
        JkProcess process = JkProcess.of(nativeImageExe);
        List<String> params = getNativeImageParams(outputFile.toString(), JkPathSequence.of(classpath).toPath());
        String paramsString = String.join( " ", params);
        if (paramsString.length() <= 2048) {  // error thrown when cmd line is too large
            process.addParams(params);
        } else {
            Path tempArgFile = JkUtilsPath.createTempFile("jeka-native-image-cp", ".txt");
            if (JkUtilsSystem.IS_WINDOWS) {
                paramsString = paramsString.replace("\\", "\\\\");
            }
            JkPathFile.of(tempArgFile).write(paramsString);
            process.addParams("@" + tempArgFile);
        }

        if (!JkLog.isVerbose()) {
            JkLog.info("Invoking nativeImage tool. This can takes several minutes. Please be patient.");
            JkLog.info("Use the `--verbose` option to show progress during the build. Build started at %s.", JkUtilsTime.now("HH:mm:ss"));
        }
        process = process
                .addParams("--no-fallback")
                .addParamsIf(!JkUtilsString.isBlank(mainClass), "-H:Class=" + mainClass)
                .addParams("-o",  outputFile.toString())
                .setLogCommand(JkLog.isDebug())
                .setInheritIO(false)
                .setLogWithJekaDecorator(JkLog.isVerbose())
                .setDestroyAtJvmShutdown(true);
        process.exec();
        JkLog.info("Generated in %s", outputFile);
        JkLog.endTask();
    }

    public List<String> getNativeImageParams(String outputFile, String classpathAsString) {
        List<String> params = new LinkedList<>();

        if (!JkUtilsString.isBlank(classpathAsString)) {
            params.add("-classpath");
            params.add(classpathAsString);
        }

        if (staticLink == StaticLink.MUSL) {
            params.add("--static");
            params.add("--libc=musl");
        } else if (staticLink == StaticLink.MOSTLY) {
            params.add("--static-nolibc");
        }
        params.add("--no-fallback");
        if (JkLog.isDebug()) {
            params.add("--verbose");
        }
        if (!JkUtilsString.isBlank(mainClass)) {
            params.add("-H:Class=" + mainClass);
        }
        params.add("-o");
        params.add(outputFile);

        // Resource bundles
        boolean hasMessageBundle = false;
        final Path resourceBundle;
        if (!classpath.isEmpty()) {
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
        if (hasMessageBundle) {
            params.add("-H:IncludeResourceBundles=MessagesBundle");
        }

        // All resources
        if (includesAllResources) {
            String regexp = "^(?!.*\\.class$).*$";
            params.add("-H:IncludeResources=" + regexp);
        }

        String reachabilityArg = reachabilityMetadata.repoDirsToIncludeAsString();
        if (!reachabilityArg.isEmpty()) {
            params.add("-H:ConfigurationFileDirectories="+reachabilityArg);
        }
        params.addAll(this.extraParams);
        return params;
    }

    public List<Path> getAotMetadataRepoPaths() {
        return reachabilityMetadata.repoDirsToInclude();
    }

    private static Path toolPath() {
        Path candidate = currentToolPath();
        if (Files.exists(candidate)) {
            return candidate;
        }
        Path graalvmHome = JkUtilsJdk.getJdk("graalvm", DEFAULT_GRAALVM_VERSION);
        return toolPath(graalvmHome);
    }

    private static Path currentToolPath() {
        return toolPath(JkJavaProcess.CURRENT_JAVA_HOME);
    }

    private static Path toolPath(Path javaHome) {
        return JkUtilsSystem.IS_WINDOWS ?
                javaHome.resolve("bin/native-image.cmd") :
                javaHome.resolve("bin/native-image");
    }

    public class ReachabilityMetadata {

        private boolean useRepo = true;

        // The version of the metadata repo to download.
        private String repoVersion = DEFAULT_REPO_VERSION;

        // repos for downloading reachability metadata
        private JkRepoSet downloadRepos = JkRepo.ofMavenCentral().toSet();

        private Path extractDir = Paths.get(JkConstants.OUTPUT_PATH + "/graalvm-reachability-metadata-repo");

        // Dependencies for which looking for reachability metadata.
        // This should contain all transitive dependencies used at runtime
        private Collection<JkCoordinate> dependencies;

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

        public ReachabilityMetadata setDependencies(Collection<JkCoordinate> dependencies) {
            this.dependencies =  dependencies;
            return this;
        }

        public ReachabilityMetadata setDownloadRepos(JkRepoSet downloadRepos) {
            this.downloadRepos = downloadRepos;
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
            if (dependencies != null) {
                JkLog.verbose("Extracting reachability metadata from resolved dependencies");
                return dependencies.stream()
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
                        "Unable to find any reachability metadata.");
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
                    JkLog.verbose("Reachability metadata could not find version for %s. Use latest version %s instead.",
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
