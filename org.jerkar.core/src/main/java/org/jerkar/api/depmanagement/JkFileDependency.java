package org.jerkar.api.depmanagement;

import org.jerkar.api.utils.JkUtilsPath;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependencies that can directly provide files without passing by an
 * external medium.
 *
 * @author Jerome Angibaud
 */
public interface JkFileDependency extends JkDependency {
    /**
     * Returns files constituting this file dependencies.
     */
    @Deprecated
    default List<File> files() {
        return JkUtilsPath.filesOf(paths());
    }

    List<Path> paths();
}
