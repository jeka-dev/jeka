package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Identifier for a dependency of a project. It can be a either :
 * <ul>
 * <li>An external module as <code>org.hibernate:hibernate-core:3.0.+</code>,</li>
 * <li>A project inside a multi-project build,</li>
 * <li>Some files on the file system.</li>
 * </ul>
 * Each dependency is associated with a scope mapping to determine precisely in
 * which scenario the dependency is necessary.
 *
 * @author Jerome Angibaud
 */
public abstract class JkDependency implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dependencies that can directly provide files without passing by an
     * external medium.
     *
     * @author Jerome Angibaud
     */
    public static abstract class JkFileDependency extends JkDependency {

        private static final long serialVersionUID = 1L;

        /**
         * Returns files constituting this file dependencies.
         */
        public abstract List<File> files();

    }

}
