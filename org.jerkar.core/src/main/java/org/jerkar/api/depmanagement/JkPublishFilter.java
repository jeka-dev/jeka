package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Defines filter to accept or not module to be published on a given
 * {@link JkPublishRepo}
 * 
 * @author Jerome Angibaud
 */
public interface JkPublishFilter extends Serializable {

    boolean accept(JkVersionedModule versionedModule);

    public static final JkPublishFilter ACCEPT_ALL = new JkPublishFilter() {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean accept(JkVersionedModule versionedModule) {
	    return true;
	}

    };

    public static final JkPublishFilter ACCEPT_SNAPSHOT_ONLY = new JkPublishFilter() {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean accept(JkVersionedModule versionedModule) {
	    return versionedModule.version().isSnapshot();
	}

    };

    public static final JkPublishFilter ACCEPT_RELEASE_ONLY = new JkPublishFilter() {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean accept(JkVersionedModule versionedModule) {
	    return !versionedModule.version().isSnapshot();
	}

    };
}