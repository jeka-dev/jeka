package org.jerkar.api.depmanagement;

import java.util.Date;

interface InternalPublisher {

	void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate);

	void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies);

	boolean hasMavenPublishRepo();

	boolean hasIvyPublishRepo();
}
