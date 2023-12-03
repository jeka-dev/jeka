package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class EngineCompilationUpdateTracker {

    private final static String LAST_UPDATE_FILE_NAME = "def-hash.txt";

    private final Path projectBaseDir;

    // When a subproject is outdated then all projects are considered outdated as well
    private static boolean globallyOutdated;

    EngineCompilationUpdateTracker(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir;
    }

    boolean isOutdated() {
        if (globallyOutdated) {
            JkLog.trace("Compilation cache outdated.");
            return true;
        }
        boolean result = isWorkOutdated();
        JkLog.trace("Cached compilation outdated : %s", result);
        globallyOutdated = result;
        return result;
    }

    void updateCompileFlag() {
        String hash = hashString();
        flagFile().deleteIfExist().createIfNotExist().write(hash.getBytes(StandardCharsets.UTF_8));
    }

    void deleteCompileFlag() {
        flagFile().deleteIfExist();
    }

    private boolean isWorkOutdated() {
        if (!flagFile().exists()) {
            return true;
        }
        String currentHash = hashString();
        String flaggedHash = flagFile().readAsString();
        return !flaggedHash.equals(currentHash);
    }

    private long lastModifiedAccordingFileAttributes() {
        Path def = projectBaseDir.resolve(JkConstants.DEF_DIR);
        Stream<Path> stream = JkPathTree.of(def).stream();
        return stream
                .filter(path -> !Files.isDirectory(path))
                .peek(path -> JkLog.trace("read file attribute of " + path))
                .map(path -> JkUtilsPath.getLastModifiedTime(path))
                .map(optional -> optional.orElse(System.currentTimeMillis()))
                .reduce(0L, Math::max);
    }



    private String hashString() {
        String md5 = JkPathTree.of(projectBaseDir.resolve(JkConstants.DEF_DIR)).checksum("md5");
        return md5 + ":" + JkJavaVersion.ofCurrent();
    }

    private JkPathFile flagFile() {
        return JkPathFile.of(projectBaseDir.resolve(JkConstants.WORK_PATH).resolve(LAST_UPDATE_FILE_NAME));
    }


    boolean isMissingBinaryFiles() {
        Path work = projectBaseDir.resolve(JkConstants.DEF_BIN_DIR);
        Path def = projectBaseDir.resolve(JkConstants.DEF_DIR);
        return JkPathTree.of(work).count(Integer.MAX_VALUE, false) <
                JkPathTree.of(def).count(Integer.MAX_VALUE, false);
    }


}
