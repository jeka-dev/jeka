package org.jerkar.api.file;

import java.io.File;

/**
 * Provides an information about its localisation on the file system.
 */
public interface JkFileSystemLocalizable {

    /**
     * Returns the base directory of this object.
     */
    File baseDir();
}
