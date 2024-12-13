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

package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkConsoleSpinner;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a wrapper for executing commands and managing the Node.js distribution.
 */
public class JkNodeJs {

    public static final String DEFAULT_NODE_VERSION = "20.10.0";

    private static final String DOWNLOAD_BASE_URL = "https://nodejs.org/dist/";

    // Directory of NodeJs distribution
    private Path installDir;

    private String version = DEFAULT_NODE_VERSION;

    private Path workingDir = Paths.get("");

    private JkNodeJs(Path installDir) {
        this.installDir = installDir;
    }

    /**
     * Creates a {@link JkNodeJs} wrapping on a nodeJs distribution located in the specified install dir.
     * @param nodeJsInstallDir Path to nodeJs installation directory
     */
    public static JkNodeJs of(Path nodeJsInstallDir) {
        return new JkNodeJs(nodeJsInstallDir);
    }

    /**
     * Creates a {@link JkNodeJs} wrapping the specified version.</p>
     *
     * Jeka caches nodeJs distribution in a specific directory. If the specified version is not
     * present in cache, it is downloaded automatically.
     *
     * @param version Version of nodeJs to use.
     */
    public static JkNodeJs ofVersion(
            @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2") String version) {
        JkNodeJs result = of(null);
        result.version = version;
        return result;
    }

    /**
     * Creates a {@link JkNodeJs} wrapping the default version.
     * @see #ofVersion(String) 
     */
    public static JkNodeJs ofDefaultVersion() {
        return ofVersion(DEFAULT_NODE_VERSION);
    }

    /**
     * Sets the working directory from where {@link #npm(String)} and {@link #npx(String)} should be run.
     */
    public JkNodeJs setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    /**
     * Returns the working directory from where npm and npx commands should be run.
     */
    public Path getWorkingDir() {
        return workingDir;
    }

    /**
     * Returns the version of the nodeJs distribution.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Executes the specified npm command line.
     * @param commandLine Command line to be executed by <i>npm</i>. The 'npm' command should
     *                    not be included in the command line.
     */
    public JkNodeJs npm(String commandLine) {
        String cmd = JkUtilsSystem.IS_WINDOWS ? "npm.cmd" : "bin/npm";
        String[] params = JkUtilsString.parseCommandline(commandLine);
        createProcess(workingDir, cmd).addParams(params).exec();
        return this;
    }

    /**
     * Executes the specified npx command line.
     * @param commandLine Command line to be executed by <i>npx</i>. The 'npx' command should
     *                    not be included in the command line.
     */
    public JkNodeJs npx(String commandLine) {
        String cmd = JkUtilsSystem.IS_WINDOWS ? "npx.cmd" : "bin/npx";
        String[] params = JkUtilsString.parseCommandline(commandLine);
        createProcess(workingDir, cmd).addParams(params).exec();
        return this;
    }

    /**
     * Executes the specified 'npm or npx' command line. The command line must include the 'npm'
     * pr 'npx' command'.
     * @param commandLine mMst start with either 'npm' nor 'npx'.
     */
    public JkNodeJs exec(String commandLine) {
        String command = JkUtilsString.substringBeforeFirst(commandLine," ").trim();
        String args = JkUtilsString.substringAfterFirst(commandLine," ").trim();
        if ("npm".equals(command)) {
            return npm(args);
        } else if ("npx".equals(command)) {
            return npx(args);
        } else {
            throw new IllegalArgumentException("For nodeJs, command line should start with either 'npm' nor 'npx'. Was "
                    + commandLine);
        }
    }

    /**
     * Configures the specified project to include a Node.js build right after main compilation.
     *
     * @param project The project to configure
     * @param jsAppBaseDir The path of the Node.js subproject (relative to the base dir).
     * @param distDir The path, relative to <code>jsAppBaseDir</code> of the directory containing the build result.
     * @param copyToDir If not empty, the result of the client  build will be copied to this directory in class dir (e.g. 'static').
     * @param buildCommands The commands (npm o npx) to execute to build the Node.js project.
     */
    public JkNodeJs configure(JkProject project, String jsAppBaseDir, String distDir, String copyToDir,
                              List<String> buildCommands, List<String> testCommands) {
        Path jsBaseDir = project.getBaseDir().resolve(jsAppBaseDir);
        Path buildJsDir = jsBaseDir.resolve(distDir);

        project.compilation.postCompileActions.append("build-js", () -> {
            this.setWorkingDir(jsBaseDir);
            JkConsoleSpinner.of("Building NodeJs application")
                    .run(() -> buildCommands.forEach(this::exec));
            JkLog.info("JS project built in %s", buildJsDir);
            if (!JkUtilsString.isBlank(copyToDir)) {
                Path target = project.compilation.layout.resolveClassDir().resolve(copyToDir);
                JkPathTree.of(buildJsDir).copyTo(target, StandardCopyOption.REPLACE_EXISTING);
                JkLog.info("Build copied to %s", target);
            }

        });
        if (!testCommands.isEmpty()) {
            project.testing.postActions.append("test-js", () -> {
                this.setWorkingDir(jsBaseDir);
                JkConsoleSpinner.of("Testing NodeJs application")
                        .run(() -> testCommands.forEach(this::exec));
                JkLog.info("JS test successful");
            });
        }
        return this;
    }

    private synchronized Path installDir() {
        if (installDir == null) {
            installDir = getDistribPath(version);
            if (!isBinaryPresent(installDir)) {
                download(version);
            }
        }
        return installDir;
    }

    private JkProcess createProcess(Path workingDir, String cmdName) {
        String path = System.getenv("PATH");
        Path commandFile = installDir().resolve(cmdName);
        Path nodeDir = commandFile.getParent();
        String pathVar = nodeDir.toString() + File.pathSeparator + path;
        return JkProcess.of(commandFile.toString())
                .setWorkingDir(workingDir)
                .setFailOnError(true)
                .setEnv("PATH", pathVar)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true);
    }

    private static boolean isBinaryPresent(Path installDir) {
        if (JkUtilsSystem.IS_WINDOWS) {
            return Files.exists(installDir.resolve("npm.cmd"));
        }
        return Files.exists(installDir.resolve("bin/npm"));
    }

    private static void download(String version) {
        JkPathFile tempZip = JkPathFile.of(JkUtilsPath.createTempFile("nodejs-downloded", ""));
        String url = constructDownloadUrl(version);
        JkLog.verbose("Downloading %s...", url);
        tempZip.fetchContentFrom(constructDownloadUrl(version));
        Path distribPath = getDistribPath(version);
        JkLog.verbose("unpack %s to %s", url, distribPath);
        if (JkUtilsSystem.IS_WINDOWS) {
            try (JkZipTree zipTree = JkZipTree.of(tempZip.get())) {
                zipTree.goTo(nodeArchiveFolderName(version)).copyTo(distribPath, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                JkUtilsPath.deleteFile(tempZip.get());
            }
        } else {
            Path distribParent = distribPath.getParent();
            JkUtilsPath.createDirectories(distribParent);
            JkProcess.of("tar", "-xf", tempZip.toString(), "-C", distribParent.toString())
                    .setLogCommand(true)
                    .run();
            Path extractDir = distribParent.resolve(nodeArchiveFolderName(version));
            try {
                Files.move(extractDir, extractDir.resolveSibling(version), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            JkUtilsPath.deleteFile(tempZip.get());
        }
    }

    private static String constructDownloadUrl(String version) {
        String baseUrl = DOWNLOAD_BASE_URL + "v" + version + "/" + nodeArchiveFolderName(version);
        if (JkUtilsSystem.IS_WINDOWS) {
            return baseUrl + ".zip";
        }
        return baseUrl + ".tar.gz";
    }

    private static String nodeArchiveFolderName(String version) {
        String baseName = "node-v" + version + "-";
        if (JkUtilsSystem.IS_WINDOWS) {
            baseName = baseName + "win-";
            String arch = JkUtilsSystem.getProcessor().is64Bit() ? "x64" : "x86";
            return baseName + arch;
        } else if (JkUtilsSystem.IS_MACOS) {
            baseName = baseName + "darwin-";
            String arch = JkUtilsSystem.getProcessor().isAarch64() ? "arm64" : "x64";
            return baseName + arch;
        } else if (JkUtilsSystem.IS_LINUX) {
            baseName = baseName + "linux-";
            String arch = JkUtilsSystem.getProcessor().isAarch64() ? "arm64" : "x64";
            return baseName + arch;
        } else {
            throw new IllegalStateException("Unknown operating system " + System.getProperty("os.name"));
        }
    }

    private static Path getDistribPath(String version) {
        return JkLocator.getCacheDir().resolve("nodejs").resolve(version);
    }

}
