package org.jerkar.api.depmanagement;

import java.util.Date;

interface InternalPublisher {

    void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies,
	    JkScopeMapping defaultMapping, Date deliveryDate, JkVersionProvider resolvedVersion);

    void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies);

    boolean hasMavenPublishRepo();

    boolean hasIvyPublishRepo();
}
