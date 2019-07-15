package dev.jeka.core.api.depmanagement;

/**
 * Defines filter to accept or not module to be published on a given {@link JkRepo}
 * 
 * @author Jerome Angibaud
 */
@FunctionalInterface
public interface JkPublishFilter {

    /**
     * Returns <code>true</code> if this filter should andAccept the specified versioned module.
     */
    boolean accept(JkVersionedModule versionedModule);

    /**
     * A filter accepting everything.
     */
    JkPublishFilter ACCEPT_ALL = versionedModule -> true;

    /**
     * A filter accepting only snapshot versioned module.
     */
    JkPublishFilter ACCEPT_SNAPSHOT_ONLY =
            versionedModule -> versionedModule.getVersion().isSnapshot();

    /**
     * A filter accepting only non-snapshot versioned module.
     */
    JkPublishFilter ACCEPT_RELEASE_ONLY =
            versionedModule -> !versionedModule.getVersion().isSnapshot();
}