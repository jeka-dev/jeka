package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;

/**
 * Interface standing for a dependency. It can be a either :
 * <ul>
 * <li>An external module as <code>org.hibernate:hibernate-core:3.0.1</code></li>
 * <li>A project inside a multi-project build</li>
 * <li>Some files on the file system</li>
 * <li>A process that produces 0 or n files</li>
 * </ul>
 *
 * @author Jerome Angibaud
 */
public interface JkDependency {

    /**
     * In the IDE, a dependency can be provided by a project exporting it. When generating IDE metadata files
     * (.classpath, xxx.iml), its preferable to mention only the project exporting it rather then dependency
     * itself.
     * This method simply returns <code>null</code> when the dependencies is not coming from another project.
     */
    Path getIdeProjectDir();

    /**
     * Returns a dependency identical to this one but with the specified project base dir.
     * @see #getIdeProjectDir()
     */
    JkDependency withIdeProjectDir(Path path);


}
