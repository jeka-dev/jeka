package org.jerkar.api.file;

import java.nio.file.Path;

/**
 * Provides an information about its localisation on the file ofSystem.
 */
public interface JkFileSystemLocalizable {

    /**
     * Returns the base directory of this object.
     */
    Path getBaseDir();
}
