package org.jerkar.publishing;

import java.util.Date;

import org.jerkar.JkBuild;
import org.jerkar.JkClassLoader;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.internal.ivy.JkIvyPublisher;

public final class JkPublisher {

	private static final String IVY_PUB_CLASS = "org.jerkar.internal.ivy.JkIvyPublisher";

	private static final JkClassLoader IVY_CLASS_LOADER = JkIvyPublisher.CLASSLOADER;

	private static final boolean IN_CLASSLOADER = JkClassLoader.current().isDefined(IVY_PUB_CLASS);

	private final Object ivyPublisher;

	private JkPublisher(Object jkIvyPublisher) {
		super();
		this.ivyPublisher = jkIvyPublisher;
	}

	public static JkPublisher of(JkPublishRepos publishRepos, JkBuild build) {
		if (!IN_CLASSLOADER) {
			final Object ivyPub = JkIvyPublisher.CLASSLOADER.invokeStaticMethod(false, IVY_PUB_CLASS, "of", publishRepos,  build.ouputDir().root());
			return new JkPublisher(ivyPub);
		}
		return new JkPublisher(JkIvyPublisher.of(publishRepos,  build.ouputDir().root()));
	}

	public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		if (!IN_CLASSLOADER) {
			IVY_CLASS_LOADER.invokeInstanceMethod(false, ivyPublisher, "publishToIvyRepo", publication, dependencies, defaultScope, defaultMapping, deliveryDate);
		} else {
			((JkIvyPublisher) this.ivyPublisher).publishToIvyRepo(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
		}
	}

	public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate) {
		if(!IN_CLASSLOADER) {
			IVY_CLASS_LOADER.invokeInstanceMethod(false, ivyPublisher, "publishToMavenRepo", versionedModule, publication, dependencies, deliveryDate);
		} else {
			((JkIvyPublisher) this.ivyPublisher).publishToMavenRepo(versionedModule, publication, dependencies, deliveryDate);
		}
	}

	public boolean hasMavenPublishRepo() {
		if (!IN_CLASSLOADER) {
			return (Boolean) IVY_CLASS_LOADER.invokeInstanceMethod(false, ivyPublisher, "hasMavenPublishRepo");
		}
		return ((JkIvyPublisher) this.ivyPublisher).hasMavenPublishRepo();
	}

	public boolean hasIvyPublishRepo() {
		if (!IN_CLASSLOADER) {
			return (Boolean) IVY_CLASS_LOADER.invokeInstanceMethod(false, ivyPublisher, "hasIvyPublishRepo");
		}
		return ((JkIvyPublisher) this.ivyPublisher).hasIvyPublishRepo();
	}


}
