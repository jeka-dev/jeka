package dev.jeka.core.api.file;

import java.nio.file.Path;

/**
 * Provides an information about its localisation on the file system.
 */
public interface JkFileSystemLocalizable {

    /**
     * Returns the base directory of this object.
     */
    Path getBaseDir();
}
