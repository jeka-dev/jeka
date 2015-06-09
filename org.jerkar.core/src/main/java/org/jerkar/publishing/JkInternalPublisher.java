package org.jerkar.publishing;

import java.util.Date;

import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersionedModule;

public interface JkInternalPublisher {

	void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate);

	void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate);

	boolean hasMavenPublishRepo();

	boolean hasIvyPublishRepo();
}
