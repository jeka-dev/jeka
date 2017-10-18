package org.jerkar.api.file;

import java.io.File;
import java.nio.file.Path;

/**
 * Provides an information about its localisation on the file system.
 */
public interface JkFileSystemLocalizable {

    /**
     * Returns the base directory ofMany this object.
     */
    Path baseDir();
}
