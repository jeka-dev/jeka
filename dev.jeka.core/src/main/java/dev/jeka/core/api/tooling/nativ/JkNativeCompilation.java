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
 *
 * <p>
 * This class enables users to generate a native executable by providing:
 * <ul>
 *   <li>A list of JAR files</li>
 *   <li>Optional configuration parameters</li>
 * </ul>
 * </p>
 *
 * <h3>Native Compilation Flow</h3>
 * <p>
 * The process of generating a native executable involves several steps:
 * </p>
 * <ol>
 *   <li>Fetch the AOT (Ahead Of Time) metadata repository.
 *       This provides additional metadata to help the compiler.</li>
 *   <li>Prepare the command-line arguments needed for the GraalVM
 *       <code>nativeImage</code> program.</li>
 *   <li>Verify the availability of GraalVM on the system.
 *       Download it if not already present.</li>
 *   <li>Use the GraalVM <code>nativeImage</code> program to compile
 *       the JAR files into a native executable.</li>
 * </ol>
 *
 * <h3>Configuration Options</h3>
 * <p>
 * Users can configure the native compilation process via the following options:
 * </p>
 * <ul>
 *   <li>Static linking of the <code>libc</code> library.
 *       Levels: <code>NONE</code>, <code>MOSTLY</code>, and <code>MUSL</code>.</li>
 *   <li>Main class: Specify the main class to be executed in the
 *       resulting native executable.</li>
 *   <li>Resource inclusion: Decide whether to bundle all runtime resources into the executable.</li>
 *   <li>Raw parameters: Provide custom arguments to the GraalVM
 *       <i>nativeImage</i> command for advanced setup.</li>
 * </ul>
 *
 * <h3>GraalVM Setup</h3>
 * <p>
 * The GraalVM distribution used for native compilation is selected based on the following precedence:
 * </p>
 * <ul>
 *   <li>If the <code>JEKA_GRAALVM_HOME</code> environment variable is defined, its value is used as the GraalVM path.</li>
 *   <li>
 *     If the environment variable starts with <code>DOWNLOAD_</code> (e.g., <code>DOWNLOAD_21</code>),
 *     GraalVM is downloaded from the internet if it is not already present in the local Jeka cache.
 *   </li>
 *   <li>
 *     If running on an existing GraalVM, that version is used automatically unless otherwise specified.
 *   </li>
 *   <li>If no other configuration is provided, a hardcoded GraalVM version is downloaded from the internet.</li>
 * </ul>
 *
 * <h3>Using the Class</h3>
 * <p>
 * After configuring the input JARs and compilation options, use the <code>make</code> method
 * to start the compilation process. The resulting executable will be produced in the output path.
 * </p>
 */
public class JkNativeCompilation {

    public static final String DEFAULT_GRAALVM_VERSION = "23";;

    public static final String DEFAULT_REPO_VERSION =  "0.10.3";

    private static final String JEKA_GRAAALVM_HOME = "JEKA_GRAAALVM_HOME";

    private static final String DOWNLOAD_ = "DOWNLOAD_";

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

    public List<Path> getAotMetadataRepoPaths() {
        return reachabilityMetadata.repoDirsToInclude();
    }

    /**
     * Generates a native image at the specified file location.
     */
    public void make(Path outputFile) {
        JkLog.startTask("compile-native-executable");

        String nativeImageExe = toolPath().toString();
        JkLog.verbose("Use native-image executable %s for compiling to native.", nativeImageExe);
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
            JkLog.info("Use the `--verbose` option to show progress during the build. Native compilation started at %s.", JkUtilsTime.now("HH:mm:ss"));
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
        JkLog.info("Native executable generated at %s", outputFile);
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

    private static Path toolPath() {
        String envValue = System.getenv("JEKA_GRAALVM_HOME");

        // Check if GraalVM home is explicitly specified
        if (!JkUtilsString.isBlank(envValue)) {

            // If defined as DOWNLOAD_XX, then download from internet
            if (envValue.startsWith(DOWNLOAD_)) {
                String version = envValue.substring(DOWNLOAD_.length());
                Path graalvmHome = JkUtilsJdk.getJdk("graalvm", version);
                JkLog.verbose("Honor environment variable %s=%s by downloading GraalVM from the internet, " +
                                "if not present in cache.", JEKA_GRAAALVM_HOME, envValue);
                return toolPath(graalvmHome);
            }
            Path result = toolPath(Paths.get(envValue));
            JkUtilsAssert.state(Files.exists(result), "Can't find %s executable from %s. Fix the value of " +
                    "%s environment variable.", result.getFileName(), envValue, JEKA_GRAAALVM_HOME);
            JkLog.verbose("Honor environment variable %s=%s by using GraalVM home: %s.", JEKA_GRAAALVM_HOME, envValue,
                    envValue);
            return result;
        }

        // Check if current JDK is GRAALVM
        Path candidate = currentJavaHomeToolPath();
        if (Files.exists(candidate)) {
            JkLog.verbose("Use native-image tool present in the current java home.");
            return candidate;
        }

        // Download from internet in last resort
        Path graalvmHome = JkUtilsJdk.getJdk("graalvm", DEFAULT_GRAALVM_VERSION);
        JkLog.verbose("Download GraalVM default version %s, if nor already in cache.", DEFAULT_GRAALVM_VERSION);
        return toolPath(graalvmHome);
    }

    private static Path currentJavaHomeToolPath() {
        return toolPath(JkJavaProcess.CURRENT_JAVA_HOME);
    }

    private static Path toolPath(Path javaHome) {
        String execName = JkUtilsSystem.IS_WINDOWS ? "native-image.cmd" : "native-image";
        Path candidate = javaHome.resolve("bin").resolve(execName);
        if (!Files.exists(candidate)) {
            candidate = javaHome.getParent().resolve("bin").resolve(execName);
        }
        return candidate;
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
