package org.jerkar.api.depmanagement;

import java.io.File;
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
    List<File> files();
}
