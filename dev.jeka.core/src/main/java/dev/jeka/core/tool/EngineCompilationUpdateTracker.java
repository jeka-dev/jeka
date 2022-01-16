package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class EngineCompilationUpdateTracker {

    private final static String LAST_UPDATE_FILE_NAME = "def-last-update-time.txt";

    private final Path projectBaseDir;

    // When a subproject is outdated then all projects are considered outdated as well
    private static boolean globallyOutdated;

    EngineCompilationUpdateTracker(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir;
    }

    boolean isOutdated() {
        if (globallyOutdated) {
            return true;
        }
        long defLastUptateTime = lastModifiedAccordingFileAttributes();
        boolean result = isWorkOutdated(fileCount(), defLastUptateTime);
        globallyOutdated = result;
        return result;
    }

    void updateCompileFlag() {
        long defLastUptateTime = lastModifiedAccordingFileAttributes();
        int fileCount = fileCount();
        JkLog.trace("Cached file count for compilation : %s", fileCount);
        writeLastUpdateFile(fileCount, defLastUptateTime, JkJavaVersion.ofCurrent());
    }

    void deleteCompileFlag() {
        flagFile().deleteIfExist();
    }

    private boolean isWorkOutdated(int fileCount, long lastModifiedAccordingFileAttributes) {
        CountTimestampAndJavaVersion countTimestampAndJavaVersion = lastModifiedAccordingFlag();
        return countTimestampAndJavaVersion.timestamp < lastModifiedAccordingFileAttributes
                || !JkJavaVersion.ofCurrent().equals(countTimestampAndJavaVersion.javaVersion)
                || countTimestampAndJavaVersion.fileCount != fileCount;
    }

    private long lastModifiedAccordingFileAttributes() {
        Path def = projectBaseDir.resolve(JkConstants.DEF_DIR);
        return JkPathTree.of(def).stream()
                .map(path -> JkUtilsPath.getLastModifiedTime(path))
                .map(optional -> optional.orElse(System.currentTimeMillis()))
                .reduce(0L, Math::max);
    }

    private void writeLastUpdateFile(int fileCount, long lastModifiedAccordingFileAttributes, JkJavaVersion javaVersion) {
        Path work = projectBaseDir.resolve(JkConstants.WORK_PATH);
        if (!Files.exists(work)) {
            return;
        }
        String infoString = String.format("%s;%s;%s", fileCount, lastModifiedAccordingFileAttributes, javaVersion);
        flagFile()
                .deleteIfExist()
                .createIfNotExist()
                .write(infoString.getBytes(StandardCharsets.UTF_8));
    }

    private JkPathFile flagFile() {
        return JkPathFile.of(projectBaseDir.resolve(JkConstants.WORK_PATH).resolve(LAST_UPDATE_FILE_NAME));
    }

    private CountTimestampAndJavaVersion lastModifiedAccordingFlag() {
        Path work = projectBaseDir.resolve(JkConstants.WORK_PATH);
        if (!Files.exists(work)) {
            return new CountTimestampAndJavaVersion(0, 0L, JkJavaVersion.ofCurrent());
        }
        Path lastUpdateFile = work.resolve(LAST_UPDATE_FILE_NAME);
        if (!Files.exists(lastUpdateFile)) {
            return new CountTimestampAndJavaVersion(0, 0L, JkJavaVersion.ofCurrent());
        }
        try {
            String content = JkUtilsPath.readAllLines(lastUpdateFile).get(0);
            String[] items = content.split(";");
            if (items.length != 3) {
                return new CountTimestampAndJavaVersion(0, 0L, JkJavaVersion.ofCurrent());
            }
            return new CountTimestampAndJavaVersion(Integer.parseInt(items[0]), Long.parseLong(items[1]),
                    JkJavaVersion.of(items[2]));
        } catch (RuntimeException e) {
            JkLog.warn("Error caught when reading file content of " + lastUpdateFile + ". " + e.getMessage() );
            return new CountTimestampAndJavaVersion(0,0L, JkJavaVersion.ofCurrent());
        }
    }

    private int fileCount() {
        return JkPathTree.of(projectBaseDir.toAbsolutePath()).andMatching("jeka/def/**", "jeka/boot/**")
                .count(5000, true);
    }

    private static class CountTimestampAndJavaVersion {

        final int fileCount;

        final long timestamp;

        final JkJavaVersion javaVersion;

        public CountTimestampAndJavaVersion(int fileCount, long timestamp, JkJavaVersion javaVersion) {
            this.fileCount = fileCount;
            this.timestamp = timestamp;
            this.javaVersion = javaVersion;
        }
    }
}
