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

	private static final String IVY_PUB_CLASS = "org.jerkar.api.depmanagement.IvyPublisher";

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

	/**
	 * Creates a {@link JkPublisher} with the specified {@link JkPublishRepo}.
	 * Pom.xml and ivy.xml will be generated in a temporary
	 *  files then deleted.
	 */
	public static JkPublisher of(JkPublishRepos publishRepos) {
		return of(publishRepos, null);
	}

	/**
	 * Publishes the specified publication to the Ivy repositories defined in this publisher
	 * @param versionedModule The module id and version to publish
	 * @param publication The content of the publication
	 * @param dependencies The dependencies of the modules (necessary to generate an ivy.xml file)
	 * @param defaultMapping
	 * @param deliveryDate The delivery date (necessary to generate an ivy.xml file)
	 * @param resolvedVersion If the dependencies contains dynamic versions (as 1.0.+) then you can
	 * mention a static version replacement. If none, you can just pass {@link JkVersionProvider#empty()}
	 */
	public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
			JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping,
			Date deliveryDate, JkVersionProvider resolvedVersion) {
		this.ivyPublisher.publishIvy(versionedModule, publication, dependencies,
				defaultMapping, deliveryDate, resolvedVersion);
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
