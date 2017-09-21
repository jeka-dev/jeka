package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Defines filter to accept or not module to be published on a given
 * {@link JkPublishRepo}
 * 
 * @author Jerome Angibaud
 */
@FunctionalInterface
public interface JkPublishFilter extends Serializable {

    /**
     * Returns <code>true</code> if this filter should accept the specified versioned module.
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
            (JkPublishFilter) versionedModule -> versionedModule.version().isSnapshot();

    /**
     * A filter accepting only non-snapshot versioned module.
     */
    JkPublishFilter ACCEPT_RELEASE_ONLY =
            (JkPublishFilter) versionedModule -> !versionedModule.version().isSnapshot();
}