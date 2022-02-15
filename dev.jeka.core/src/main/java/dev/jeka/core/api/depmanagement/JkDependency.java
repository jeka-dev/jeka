package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;

/**
 * Interface standing for a dependency. It can be either :
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
     * (.classpath, xxx.iml), it's preferable to mention only the project exporting it rather than dependency
     * itself.
     * This method simply returns <code>null</code> when the dependency is not coming from another project.
     */
    Path getIdeProjectDir();

    /**
     * Returns a dependency identical to this one but with the specified project base dir.
     * @see #getIdeProjectDir()
     */
    JkDependency withIdeProjectDir(Path path);

    /**
     * Returns <code>true</code> if the specified dependency matches with this one. <p>
     * Matching means that two matching dependencies can not be declared in a same dependency set
     * as it will be considered as a duplicate or result in a conflict. <p>
     * For example "com.google:guava:21.0" is matching with "com.google:guava:23.0" even if they are not equals.
     */
    default boolean matches(JkDependency other) {
        return this.equals(other);
    }


}
