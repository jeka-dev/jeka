package org.jerkar.publishing;

import java.util.Date;

import org.jerkar.JkBuild;
import org.jerkar.JkClassLoader;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.internal.ivy.JkInternalIvy;

public final class JkPublisher {

	private static final String IVY_PUB_CLASS = "org.jerkar.internal.ivy.IvyPublisher";

	private static final JkClassLoader IVY_CLASS_LOADER = JkInternalIvy.CLASSLOADER;

	private final JkInternalPublisher ivyPublisher;

	private JkPublisher(JkInternalPublisher jkIvyPublisher) {
		super();
		this.ivyPublisher = jkIvyPublisher;
	}

	public static JkPublisher of(JkPublishRepos publishRepos, JkBuild build) {
		final JkInternalPublisher ivyPublisher = IVY_CLASS_LOADER.transClassloaderProxy(JkInternalPublisher.class, IVY_PUB_CLASS, "of", publishRepos,  build.ouputDir().root());
		return new JkPublisher(ivyPublisher);
	}

	public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		this.ivyPublisher.publishIvy(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
	}

	public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate) {
		this.ivyPublisher.publishMaven(versionedModule, publication, dependencies, deliveryDate);
	}

	public boolean hasMavenPublishRepo() {
		return this.ivyPublisher.hasMavenPublishRepo();
	}

	public boolean hasIvyPublishRepo() {
		return this.ivyPublisher.hasIvyPublishRepo();
	}


}
