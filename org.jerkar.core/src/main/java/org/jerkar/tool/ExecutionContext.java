package org.jerkar.tool;

import java.nio.file.Path;

class ExecutionContext {

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> MASTER_BUILD = new ThreadLocal<>();

    boolean masterBuild;

    void startBuild() {
        masterBuild = MASTER_BUILD.get() != null;
        MASTER_BUILD.set(masterBuild);
    }

    void resetCurrentBuild() {
        MASTER_BUILD.set(masterBuild);
    }

    boolean isCurrentBuildMaster() {
        return masterBuild;
    }


}
