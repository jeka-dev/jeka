package build.common;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class JkNodeJs {

    private static final String DOWNLOAD_URI = "https://nodejs.org/dist/";

    private static final Path DEFAULT_INSTALL_ALL_DIST_DIR = JkLocator.getJekaUserHomeDir().resolve("cache/install/nodejs");

    private Path installAllDistDir;

    private String downloadBaseUri;

    private String version = "14.18.1";

    private Platform platform;

    private Path workingDir = Paths.get("");

    private JkProcess process;

    enum Platform {
        LINUX_X64("linux-x64", "tar.gz"),
        MACOS("darwin-x64","tar.gz"),
        WIN_X64("win-x64", "zip"),
        WIN_X86("win-x86", "zip");

        private final String  value;

        private final String extension;

        Platform(String value, String extension) {
            this.value = value;
            this.extension = extension;
        }

        public static Platform ofCurrent() {
            if (JkUtilsSystem.IS_WINDOWS) {
                return WIN_X64;
            } else if (JkUtilsSystem.IS_MACOS) {
                return MACOS;
            }
            return LINUX_X64;
        }
    }

    private JkNodeJs(Path installAllDistDir, String downloadBaseUri, String version, Platform platform) {
        this.installAllDistDir = installAllDistDir;
        this.downloadBaseUri = downloadBaseUri;
        this.version = version;
        this.platform = platform;
    }

    public static JkNodeJs of(String version) {
        return new JkNodeJs(DEFAULT_INSTALL_ALL_DIST_DIR, DOWNLOAD_URI, version, Platform.ofCurrent());
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public JkNodeJs setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    private String name() {
        return "node-v" + version.trim() + "-" + platform.value;
    }

    private Path home() {
        return installAllDistDir.resolve(name());
    }

    private String filename() {
        return name() + "." + platform.extension;
    }

    private String getDownloadUrl() {
        String slash = downloadBaseUri.endsWith("/") ? "" : "/";
        return downloadBaseUri + slash + "v" + version + "/" + filename();
    }

    private String commandFile(String commandName) {
        String platformName = JkUtilsSystem.IS_WINDOWS ? commandName + ".cmd" : commandName;
        return home().resolve(platformName).toString();
    }

    private void installIfNeeded() {
        if (Files.exists(home())) {
            return;
        }
        Path temp = JkUtilsPath.createTempFile("jeka-nodejs-", "." + platform.extension);
        JkLog.startTask("Downloading " + getDownloadUrl() + " ...");
        JkUtilsIO.copyUrlToFile(getDownloadUrl(), temp);
        JkLog.endTask();
        JkZipTree.of(temp).copyTo(installAllDistDir, StandardCopyOption.REPLACE_EXISTING);
    }

    private JkProcess process(String command) {
        return JkProcess.of(command).setWorkingDir(workingDir).setLogOutput(true).setLogCommand(true);
    }

    public void exec(String command, String ...args) {
        installIfNeeded();
        process(commandFile(command)).exec(args);
    }

    public void node(String ...args) {
        exec("node", args);
    }

    public void npm(String ...args) {
        exec("npm", args);
    }

    public void npx(String ...args) {
        exec("npx", args);
    }


}
