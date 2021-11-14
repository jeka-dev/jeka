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

    EngineCompilationUpdateTracker(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir;
    }

    boolean isOutdated() {
        long defLastUptateTime = lastModifiedAccordingFileAttributes();
        return isWorkOutdated(defLastUptateTime);
    }

    void updateCompileFlag() {
        long defLastUptateTime = lastModifiedAccordingFileAttributes();
        writeLastUpdateFile(defLastUptateTime, JkJavaVersion.ofCurrent());
    }

    private boolean isWorkOutdated(long lastModifiedAccordingFileAttributes) {
        TimestampAndJavaVersion timestampAndJavaVersion = lastModifiedAccordingFlag();
        return timestampAndJavaVersion.timestamp < lastModifiedAccordingFileAttributes
                || !JkJavaVersion.ofCurrent().equals(timestampAndJavaVersion.javaVersion);
    }

    private long lastModifiedAccordingFileAttributes() {
        Path def = projectBaseDir.resolve(JkConstants.DEF_DIR);
        return JkPathTree.of(def).stream()
                .map(path -> JkUtilsPath.getLastModifiedTime(path))
                .map(optional -> optional.orElse(System.currentTimeMillis()))
                .reduce(0L, Math::max);
    }

    private void writeLastUpdateFile(long lastModifiedAccordingFileAttributes, JkJavaVersion javaVersion) {
        Path work = projectBaseDir.resolve(JkConstants.WORK_PATH);
        if (!Files.exists(work)) {
            return;
        }
        String infoString = Long.toString(lastModifiedAccordingFileAttributes) + ";" + javaVersion;
        JkPathFile.of(work.resolve(LAST_UPDATE_FILE_NAME))
                .deleteIfExist()
                .createIfNotExist()
                .write(infoString.getBytes(StandardCharsets.UTF_8));
    }

    private TimestampAndJavaVersion lastModifiedAccordingFlag() {
        Path work = projectBaseDir.resolve(JkConstants.WORK_PATH);
        if (!Files.exists(work)) {
            return new TimestampAndJavaVersion(0L, JkJavaVersion.ofCurrent());
        }
        Path lastUpdateFile = work.resolve(LAST_UPDATE_FILE_NAME);
        if (!Files.exists(lastUpdateFile)) {
            return new TimestampAndJavaVersion(0L, JkJavaVersion.ofCurrent());
        }
        try {
            String content = JkUtilsPath.readAllLines(lastUpdateFile).get(0);
            String[] items = content.split(";");
            return new TimestampAndJavaVersion(Long.parseLong(items[0]), JkJavaVersion.of(items[1]));
        } catch (RuntimeException e) {
            JkLog.warn("Error caught when reading file content of " + lastUpdateFile + ". " + e.getMessage() );
            return new TimestampAndJavaVersion(0L, JkJavaVersion.ofCurrent());
        }
    }

    private static class TimestampAndJavaVersion {

        final long timestamp;

        final JkJavaVersion javaVersion;

        public TimestampAndJavaVersion(long timestamp, JkJavaVersion javaVersion) {
            this.timestamp = timestamp;
            this.javaVersion = javaVersion;
        }
    }
}
