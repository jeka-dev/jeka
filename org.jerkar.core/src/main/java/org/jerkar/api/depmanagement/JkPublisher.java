package org.jerkar.api.depmanagement;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.system.JkLocator;

/**
 * A class to publish artifacts on repositories. According the nature of the
 * repository (Maven or Ivy) the publisher will also create the necessary
 * metadata (pom.xml, metadata.xml, ivy.xml, checksums, ...).
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
     * Creates a {@link JkPublisher} with the specified {@link JkPublishRepo}.
     */
    public static JkPublisher of(JkPublishRepo publishRepo) {
        return of(JkPublishRepos.of(publishRepo));
    }

    /**
     * Creates a publisher that publish locally under <code></code>[USER HOME]/.jerkar/publish</code> folder.
     */
    public static JkPublisher local() {
        final Path file = JkLocator.jerkarUserHomeDir().resolve("maven-publish-dir");
        return JkPublisher.of(JkRepo.maven(file).asPublishRepo());
    }

    /**
     * Creates a {@link JkPublisher} with the specified {@link JkPublishRepo}
     * and output directory. The output directory is the place where pom.xml and
     * ivy.xml are generated.
     */
    public static JkPublisher of(JkPublishRepos publishRepos, Path outDir) {
        final InternalPublisher ivyPublisher = IVY_CLASS_LOADER.transClassloaderProxy(
                InternalPublisher.class, IVY_PUB_CLASS, "of", publishRepos, outDir.toFile());
        return new JkPublisher(ivyPublisher);
    }

    /**
     * Creates a {@link JkPublisher} with the specified {@link JkPublishRepo}.
     */
    public static JkPublisher of(JkPublishRepos publishRepos) {
        return of(publishRepos, (Path) null);
    }

    /**
     * Publishes the specified publication to the Ivy repositories defined in
     * this publisher
     *
     * @param versionedModule
     *            The module id and projectVersion to publish
     * @param publication
     *            The content of the publication
     * @param dependencies
     *            The dependencies of the modules (necessary to generate an
     *            ivy.xml file)
     * @param defaultMapping
     * @param deliveryDate
     *            The delivery date (necessary to generate an ivy.xml file)
     * @param resolvedVersion
     *            If the dependencies contains dynamic versions (as 1.0.+) then
     *            you can mention a static projectVersion replacement. If none, you can
     *            just pass {@link JkVersionProvider#empty()}
     */
    public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                           JkDependencySet dependencies, JkScopeMapping defaultMapping,
                           Instant deliveryDate, JkVersionProvider resolvedVersion) {
        this.ivyPublisher.publishIvy(versionedModule, publication, dependencies, defaultMapping,
                deliveryDate, resolvedVersion);
    }

    /**
     * Publishes the specified publication on the Maven repositories of this
     * publisher.
     *
     * @param versionedModule
     *            The target moduleId and projectVersion for the specified publication
     * @param publication
     *            The content of the publication
     * @param dependencies
     *            The dependencies to specify in the generated pom file.
     */
    public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
            JkDependencySet dependencies) {
        this.ivyPublisher.publishMaven(versionedModule, publication, dependencies.modulesOnly());
    }

    /**
     * Publishes all artifact files for the specified artifact producer for the specified versioned module.
     *
     * @param versionedModule The target moduleId and projectVersion for the specified publication
     * @param dependencies The dependencies to specify in the generated pom file.
     * @param extraPublishInfo Extra information about authors, licensing, source control management, ...
     * @param artifactLocator Object producing artifacts to be deployed. This object is used only to find
     *               artifact files. If an artifact files is not present, it is not created by this method.
     */
    public void publishMaven(JkVersionedModule versionedModule, JkArtifactLocator artifactLocator,
                             Set<JkArtifactId> excludedArtifacts,
                             JkDependencySet dependencies, JkMavenPublicationInfo extraPublishInfo) {
        JkMavenPublication publication = JkMavenPublication.of(artifactLocator, excludedArtifacts).with(extraPublishInfo);
        this.ivyPublisher.publishMaven(versionedModule, publication, dependencies.modulesOnly());
    }

    /**
     * Returns <code>true</code> if this publisher contains Maven reposirories.
     */
    public boolean hasMavenPublishRepo() {
        return this.ivyPublisher.hasMavenPublishRepo();
    }

    /**
     * Returns <code>true</code> if this publisher contains Ivy reposirories.
     */
    public boolean hasIvyPublishRepo() {
        return this.ivyPublisher.hasIvyPublishRepo();
    }

}
