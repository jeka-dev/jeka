package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.Date;

import org.jerkar.api.java.JkClassLoader;

/**
 * A class to publish artifacts on repositories. According the nature of the repository (Maven or Ivy)
 * the publisher will also create the necessary metadata (pom.xml, metadata.xml, ivy.xml, checksums, ...).
 *
 * @author Jerome Angibaud
 */
public final class JkPublisher {

	private static final String IVY_PUB_CLASS = "org.jerkar.api.internal.ivy.IvyPublisher";

	private static final JkClassLoader IVY_CLASS_LOADER = IvyClassloader.CLASSLOADER;

	private final InternalPublisher ivyPublisher;

	private JkPublisher(InternalPublisher jkIvyPublisher) {
		super();
		this.ivyPublisher = jkIvyPublisher;
	}

	/**
	 * Creates a {@link JkPublisher} with the specified {@link JkPublishRepo} and output directory.
	 * The output directory is the place where pom.xml and ivy.xml are generated.
	 */
	public static JkPublisher of(JkPublishRepos publishRepos, File outDir) {
		final InternalPublisher ivyPublisher = IVY_CLASS_LOADER.transClassloaderProxy(InternalPublisher.class, IVY_PUB_CLASS, "of", publishRepos, outDir);
		return new JkPublisher(ivyPublisher);
	}

	public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		this.ivyPublisher.publishIvy(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
	}

	/**
	 * Publishes the specified publication on the Maven repositories of this publisher.
	 * @param versionedModule The target moduleId and version for the specified publication
	 * @param publication The content of the publication
	 * @param dependencies The dependencies to specify in the generated pom file.
	 */
	public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies) {
		this.ivyPublisher.publishMaven(versionedModule, publication, dependencies);
	}

	public boolean hasMavenPublishRepo() {
		return this.ivyPublisher.hasMavenPublishRepo();
	}

	public boolean hasIvyPublishRepo() {
		return this.ivyPublisher.hasIvyPublishRepo();
	}


}
