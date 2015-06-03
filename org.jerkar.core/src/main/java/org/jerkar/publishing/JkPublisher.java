package org.jerkar.publishing;

import java.util.Date;

import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.internal.ivy.JkIvyPublisher;

public final class JkPublisher {

	private final JkIvyPublisher jkIvy;

	private JkPublisher(JkIvyPublisher jkIvy) {
		super();
		this.jkIvy = jkIvy;
	}

	public static JkPublisher usingIvy(JkIvyPublisher ivy) {
		return new JkPublisher(ivy);
	}

	public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		this.jkIvy.publishToIvyRepo(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
	}

	public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate) {
		this.jkIvy.publishToMavenRepo(versionedModule, publication, dependencies, deliveryDate);
	}

	public boolean hasMavenPublishRepo() {
		return this.jkIvy.hasMavenPublishRepo();
	}

	public boolean hasIvyPublishRepo() {
		return this.jkIvy.hasIvyPublishRepo();
	}


}
