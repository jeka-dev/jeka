package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Defines filter to andAccept or not module to be published on a given
 * {@link JkRepo}
 * 
 * @author Jerome Angibaud
 */
@FunctionalInterface
public interface JkPublishFilter extends Serializable {

    /**
     * Returns <code>true</code> if this filter should andAccept the specified versioned module.
     */
    boolean accept(JkVersionedModule versionedModule);

    /**
     * A filter accepting everything.
     */
    JkPublishFilter ACCEPT_ALL = (JkPublishFilter) versionedModule -> true;

    /**
     * A filter accepting only snapshot versioned module.
     */
    JkPublishFilter ACCEPT_SNAPSHOT_ONLY =
            (JkPublishFilter) versionedModule -> versionedModule.getVersion().isSnapshot();

    /**
     * A filter accepting only non-snapshot versioned module.
     */
    JkPublishFilter ACCEPT_RELEASE_ONLY =
            (JkPublishFilter) versionedModule -> !versionedModule.getVersion().isSnapshot();
}