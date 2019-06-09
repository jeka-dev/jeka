package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.List;

/**
 * Dependencies that can directly provide files without passing by an external medium.
 *
 * @author Jerome Angibaud
 */
public interface JkFileDependency extends JkDependency {

    List<Path> getFiles();
}
