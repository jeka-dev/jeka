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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Convenient methods to deal with running JDK.
 */
public final class JkUtilsJdk {

    private JkUtilsJdk() {
        // Can not instantiate
    }

    /**
     * Returns the tool library file of the running JDK.
     */
    public static Path toolsJar() {
        final String jdkLocation = System.getProperty("java.home");
        final Path javaHome = Paths.get(jdkLocation);
        return javaHome.resolve("../lib/tools.jar").normalize().toAbsolutePath();
    }

    public static Path javaHome() {
        final String jdkLocation = System.getProperty("java.home");
        return Paths.get(jdkLocation);
    }

    public static int runningMajorVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }

    public static Path getJdk(String distrib, String majorVersion) {
        Path jdkDir = jdkPath(distrib, majorVersion);
        String javaExec = JkUtilsSystem.IS_WINDOWS ? "java.exe" : "java";
        if (Files.exists(jdkDir.resolve("bin/" + javaExec))) {
            return jdkDir;
        }
        JkLog.info("Downloading JDK " + distrib + " " + majorVersion
                + ". It may take a while ...");
        String url = jdkUrl(distrib, majorVersion);
        String ext = JkUtilsSystem.IS_WINDOWS ? ".zip" : ".tar.gz";
        String tempBaseName = "jeka-jdk-download-" + distrib + "-" + majorVersion;
        Path tempArchive = JkUtilsPath.createTempFile(tempBaseName, ext);
        JkUtilsPath.deleteIfExists(tempArchive);
        JkHttpRequest.of(url).downloadFile(tempArchive);
        Path expandDir = JkUtilsPath.createTempDirectory(tempBaseName);
        if (JkUtilsSystem.IS_WINDOWS) {
            JkUtilsZip.unzip(tempArchive, expandDir);
        } else {
            JkUtilsZip.expandTarGz(tempArchive, expandDir);
        }
        Path jdkHome = JkPathTree.of(expandDir).streamBreathFirst()
                .filter(path -> "bin".equals(path.getFileName().toString()))
                .findFirst().get().getParent();
        JkUtilsPath.createDirectories(jdkDir);
        JkPathTree.of(jdkDir).deleteRoot();
        JkUtilsPath.moveDir(jdkHome, jdkDir);
        JkUtilsPath.deleteFile(tempArchive);
        JkPathTree.of(expandDir).deleteRoot();
        return jdkDir;
    }

    private static Path jdkPath(String distrib, String majorVersion) {
        return JkLocator.getCacheDir().resolve("jdks").resolve(distrib + "-" + majorVersion);
    }

    private static String jdkUrl(String distrib, String majorVersion) {
        if (JkUtilsSystem.IS_WINDOWS) {
            return jdkDownloadUrlWindows(distrib, majorVersion);
        }
        final String libcType;
        if (JkUtilsSystem.IS_MACOS) {
            libcType = "libc";
        } else  {   // linux
            libcType = "glibc";
        }
        return jdkDownloadUrlUnix(distrib, libcType, majorVersion, getOS(), getArch());
    }

    /**
     * @param distrib corretto, graalvm,...
     * @param version 17,18,21,...
     *
     */
    private static String jdkDownloadUrlWindows(String distrib, String version) {
        String urlTemplate = "https://api.foojay.io/disco/v3.0/directuris?distro=%s&javafx_bundled=false&libc_type=c_std_lib&archive_type=zip&operating_system=windows&package_type=jdk&version=%s&architecture=x64&latest=available";
        return String.format(urlTemplate, distrib, version);
    }

    /**
     * @param distrib
     * @param version
     * @param libcType
     * @param os macos,...
     * @return
     */
    private static String jdkDownloadUrlUnix(String distrib, String libcType, String version,
                                             String os, String architecture) {
        String urlTemplate =
                "https://api.foojay.io/disco/v3.0/directuris?distro=" + distrib +
                "&javafx_bundled=false&libc_type=" + libcType +
                "&archive_type=tar.gz" +
                "&operating_system=" + os +
                "&package_type=jdk&version=" + version +
                "&architecture=" + architecture +
                "&latest=available=available";
        return String.format(urlTemplate, distrib, version);
    }

    private static String getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
        if (os.startsWith("mac") || os.startsWith("osx")) {
            return "mac";
        } else if (os.startsWith("linux")) {
            if (Files.exists(Paths.get("/etc/alpine-release"))) {
                return "alpine_linux";
            } else {
                return "linux";
            }
        } else if (os.startsWith("aix")) {
            return "aix";
        } else {
            return "unknown";
        }
    }

    public static String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
        if (arch.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x64";
        } else if (arch.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x32";
        } else if (arch.matches("^(aarch64)$")) {
            return "aarch64";
        } else if (arch.matches("^(arm)$")) {
            return "arm";
        } else if (arch.matches("^(ppc64)$")) {
            return "ppc64";
        } else if (arch.matches("^(ppc64le)$")) {
            return "ppc64le";
        } else if (arch.matches("^(s390x)$")) {
            return "s390x";
        } else if (arch.matches("^(arm64)$")) {
            return "arm64";
        } else if (arch.matches("^(riscv64)$")) {
            return "riscv64";
        } else {
            return "unknown";
        }
    }

}
