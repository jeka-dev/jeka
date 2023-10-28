package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.project.JkProject;
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

public class JkNodeJs {

    private static final String DOWNLOAD_BASE_URL = "https://nodejs.org/dist/";

    private Path installDir;

    private String version;

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
    public static JkNodeJs ofVersion(String version) {
        JkNodeJs result = of(null);
        result.version = version;
        return result;
    }

    /**
     * Sets the working directory from where {@link #npm(String)} and {@link #npx(String)} should be run.
     */
    public JkNodeJs setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    /**
     * Executes the specified npm command line.
     * @param commandLine Command line to be executed by <i>npm</i>. The 'npm' command should
     *                    not be included in the command line.
     */
    public JkNodeJs npm(String commandLine) {
        String cmd = JkUtilsSystem.IS_WINDOWS ? "npm.cmd" : "bin/npm";
        createProcess(workingDir, cmd).exec(JkUtilsString.translateCommandline(commandLine));
        return this;
    }

    /**
     * Executes the specified npx command line.
     * @param commandLine Command line to be executed by <i>npx</i>. The 'npx' command should
     *                    not be included in the command line.
     */
    public JkNodeJs npx(String commandLine) {
        String cmd = JkUtilsSystem.IS_WINDOWS ? "npx.cmd" : "bin/npx";
        createProcess(workingDir, cmd).exec(JkUtilsString.translateCommandline(commandLine));
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
     * Configures the specified project to include a nodeJs build right after main compilation.
     * @param project The project to configure
     * @param clientBaseDir The path, relative to project base dir, of the nodeJs subproject.
     * @param clientBuildDir The path, relative to @clientBuildDir, of the directory containing the build result.
     * @param buildCommands The commands (npm o npx) to execute to build the nodeJs project.
     */
    public JkNodeJs configure(JkProject project, String clientBaseDir, String clientBuildDir,
                              String ...buildCommands) {
        Path baseDir = project.getBaseDir().resolve(clientBaseDir);
        Path buildDir = baseDir.resolve(clientBuildDir);
        project.compilation.postCompileActions.append("Execute NodeJs", () -> {
            this.setWorkingDir(baseDir);
            Arrays.stream(buildCommands).forEach(this::exec);
            JkPathTree.of(buildDir).copyTo(project.compilation.layout.resolveClassDir().resolve("static"));
        });
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

    private JkProcess<?> createProcess(Path workingDir, String cmdName) {
        String path = System.getenv("PATH");
        Path commandFile = installDir().resolve(cmdName);
        Path nodeDir = commandFile.getParent();
        String pathVar = nodeDir.toString() + File.pathSeparator + path;
        return JkProcess.of(commandFile.toString())
                .setWorkingDir(workingDir)
                .setFailOnError(true)
                .setEnv("PATH", pathVar)
                .setLogCommand(true)
                .setLogOutput(true);
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
        JkLog.info("Downloading " + url + "... ");
        tempZip.fetchContentFrom(constructDownloadUrl(version));
        Path distribPath = getDistribPath(version);
        JkLog.info("unpack " + url + " to " + distribPath);
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
