package org.jerkar.api.publishing;

import java.util.Date;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.depmanagement.JkVersionedModule;

public interface JkInternalPublisher {

	void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate);

	void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate);

	boolean hasMavenPublishRepo();

	boolean hasIvyPublishRepo();
}
