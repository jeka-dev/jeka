package dev.jeka.core.tool.builtins.tooling.ide;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Folders already synced during this JeKa execution, by 'intellij: syncAll' or 'eclipse: all'.
 * Run from a parent declaring child bases, these methods run in each child, then in the parent, which scans the
 * children again: they are synced once only.
 */
final class IdeSyncedFolders {

    private static final Set<String> SYNCED = ConcurrentHashMap.newKeySet();

    /**
     * Returns true if the folder has not been synced yet for this kind of sync, and records it as synced.
     */
    static boolean markSynced(String kind, Path folder) {
        return SYNCED.add(kind + ":" + folder.toAbsolutePath().normalize());
    }

}
