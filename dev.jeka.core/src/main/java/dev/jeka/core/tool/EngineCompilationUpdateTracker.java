package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

class EngineCompilationUpdateTracker {

    private final static String LAST_UPDATE_FILE_NAME = "jeka-src-hash.txt";

    private final static String JEKA_SRC_CLASSPATH_FILE_NAME = "jeka-src-classpath.txt";

    private final static String KOTLIN_LIBS_FILE_NAME = "jeka-kotlin-libs-path.txt";

    private final Path baseDir;

    // When a subproject is outdated then all projects are considered outdated as well
    private static boolean globallyOutdated;

    EngineCompilationUpdateTracker(Path baseDir) {
        this.baseDir = baseDir;
    }

    boolean needRecompile(JkPathSequence classpath) {
        boolean result = isOutdated() || isMissingBinaryFiles() ||  !classpath.equals(readJekaSrcClasspath());
        if (result) {
            updateJekaSrcClasspath(classpath);
        }
        return result;
    }

    boolean isOutdated() {
        if (globallyOutdated) {
            JkLog.verbose("Compilation cache outdated.");
            return true;
        }
        boolean result = isWorkOutdated();
        JkLog.debug("Cached compilation outdated : %s", result);
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

    void updateKotlinLibs(JkPathSequence pathSequence) {
        pathSequence.writeTo(kotlinLibsFile());
    }

    JkPathSequence readKotlinLibsFile() {
        return JkPathSequence.readFromQuietly(kotlinLibsFile());
    }

    void updateJekaSrcClasspath(JkPathSequence pathSequence) {
        pathSequence.writeTo(jekaSrcClasspathFile());
    }

    private JkPathSequence readJekaSrcClasspath() {
        return JkPathSequence.readFrom(jekaSrcClasspathFile());
    }

    private boolean isWorkOutdated() {
        if (!flagFile().exists()) {
            return true;
        }
        String currentHash = hashString();
        String flaggedHash = flagFile().readAsString();
        return !flaggedHash.equals(currentHash);
    }

    private String hashString() {
        String md5 = JkPathTree.of(baseDir.resolve(JkConstants.JEKA_SRC_DIR)).checksum("md5");
        return md5 + ":" + JkJavaVersion.ofCurrent();
    }

    private JkPathFile flagFile() {
        return JkPathFile.of(baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(LAST_UPDATE_FILE_NAME));
    }

    private Path kotlinLibsFile() {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(KOTLIN_LIBS_FILE_NAME);
    }

    private Path jekaSrcClasspathFile() {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(JEKA_SRC_CLASSPATH_FILE_NAME);
    }

    boolean isMissingBinaryFiles() {
        Path work = baseDir.resolve(JkConstants.JEKA_SRC_CLASSES_DIR);
        Path jekaSrc = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        return JkPathTree.of(work).count(Integer.MAX_VALUE, false) <
                JkPathTree.of(jekaSrc).count(Integer.MAX_VALUE, false);
    }


}
