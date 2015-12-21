package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Defines filter to accept or not module to be published on a given
 * {@link JkPublishRepo}
 * 
 * @author Jerome Angibaud
 */
public interface JkPublishFilter extends Serializable {

    /**
     * Returns <code>true</code> if this filter should accept the specified versioned module.
     */
    boolean accept(JkVersionedModule versionedModule);

    /**
     * A filter accepting everything.
     */
    public static final JkPublishFilter ACCEPT_ALL = new JkPublishFilter() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(JkVersionedModule versionedModule) {
            return true;
        }

    };

    /**
     * A filter accepting only snapshot versioned module.
     */
    public static final JkPublishFilter ACCEPT_SNAPSHOT_ONLY = new JkPublishFilter() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(JkVersionedModule versionedModule) {
            return versionedModule.version().isSnapshot();
        }

    };

    /**
     * A filter accepting only non-snapshot versioned module.
     */
    public static final JkPublishFilter ACCEPT_RELEASE_ONLY = new JkPublishFilter() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(JkVersionedModule versionedModule) {
            return !versionedModule.version().isSnapshot();
        }

    };
}