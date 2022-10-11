package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@JkDoc("Install and run a specified version of NodeJs/npm")
public class NodeJsJkBean extends JkBean {

    private static final String BASE_URL = "https://nodejs.org/dist/";

    @JkDoc("The version of NodeJs to use")
    public String version = "16.17.0";

    @JkDoc("The command line to execute (without command name.")
    public String cmdLine;

    private Path workingDir;

    public NodeJsJkBean() {
        this.workingDir = getBaseDir();
    }

    private Path getDistribPath() {
        return JkLocator.getCacheDir().resolve("nodejs").resolve(version);
    }

    @JkDoc("Execute npm using the command line specified in 'cmdLine' property.")
   public void npm() {
        npm(this.cmdLine);
   }

    public void npm(String cmdLine) {
        Path distrib = getDistribPath();
        JkNodeJs nodeJs = JkNodeJs.of(distrib);
        if (!nodeJs.isBinaryPresent()) {
            download();
        }
        JkNodeJs.of(distrib).npm(workingDir, cmdLine);
    }

   public NodeJsJkBean setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        return this;
   }

   private void download() {
       JkPathFile tempZip = JkPathFile.of(JkUtilsPath.createTempFile("nodejs-downloded", ""));
       String url = constructDownloadUrl();
       JkLog.info("Downloading " + url + "... ");
       tempZip.fetchContentFrom(constructDownloadUrl());
       Path distribPath = getDistribPath();
       JkLog.info("unpack " + url + " to " + distribPath);
       if (JkUtilsSystem.IS_WINDOWS) {
           try (JkZipTree zipTree = JkZipTree.of(tempZip.get())) {
               zipTree.goTo(nodeArchiveFolderName()).copyTo(distribPath, StandardCopyOption.REPLACE_EXISTING);
           } finally {
               JkUtilsPath.deleteFile(tempZip.get());
           }
       } else {
           Path distribParent = distribPath.getParent();
           JkUtilsPath.createDirectories(distribParent);
           JkProcess.of("tar", "-xf", tempZip.toString(), "-C", distribParent.toString())
                   .setLogCommand(true)
                   .run();
           Path extractDir = distribParent.resolve(nodeArchiveFolderName());
           try {
               Files.move(extractDir, extractDir.resolveSibling(version), StandardCopyOption.REPLACE_EXISTING);
           } catch (IOException e) {
               throw new UncheckedIOException(e);
           }
           JkUtilsPath.deleteFile(tempZip.get());
       }

   }

   private String constructDownloadUrl() {
        String baseUrl = BASE_URL + "v" + version + "/" + nodeArchiveFolderName();
        if (JkUtilsSystem.IS_WINDOWS) {
            return baseUrl + ".zip";
        }
        return baseUrl + ".tar.gz";
   }

   private String nodeArchiveFolderName() {
       String baseName = "node-v" + version + "-";
       if (JkUtilsSystem.IS_WINDOWS) {
           baseName = baseName + "win-";
           String arch = JkUtilsSystem.getProcessor().is64Bit() ? "x64" : "x86";
           return baseName + arch;
       } else if (JkUtilsSystem.IS_MACOS) {
           baseName = baseName + "darwin-";
           String arch = JkUtilsSystem.getProcessor().isAarch64() ? "x64" : "arm64";
           return baseName + arch;
       } else if (JkUtilsSystem.IS_LINUX) {
           baseName = baseName + "linux-";
           String arch = JkUtilsSystem.getProcessor().isAarch64() ? "x64" : "arm64";
           return baseName + arch;
       } else {
           throw new IllegalStateException("Unknow operating system " + System.getProperty("os.name"));
       }
   }

}
