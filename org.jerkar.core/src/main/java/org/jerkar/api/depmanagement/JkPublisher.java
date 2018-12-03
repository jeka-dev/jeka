package org.jerkar.api.depmanagement;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkUrlClassLoader;
import org.jerkar.api.system.JkException;

/**
 * A class to publish artifacts on repositories. According the nature of the
 * repository (Maven or Ivy) the publisher will also create the necessary
 * metadata (pom.xml, metadata.xml, ivy.xml, checksums, ...).
 *
 * @author Jerome Angibaud
 */
public final class JkPublisher {

    private static final String IVY_PUB_CLASS = "org.jerkar.api.depmanagement.IvyInternalPublisher";

    private static final JkClassLoader IVY_CLASS_LOADER = IvyClassloader.CLASSLOADER;

    private final InternalPublisher internalPublisher;

    private JkPublisher(InternalPublisher internalPublisher) {
        super();
        this.internalPublisher = internalPublisher;
    }

    /**
     * Creates a {@link JkPublisher} with the specified {@link JkRepo}.
     */
    public static JkPublisher of(JkRepo repoConfig) {
        return of(JkRepoSet.of(repoConfig));
    }

    /**
     * Creates a {@link JkPublisher} with the specified {@link JkRepoSet}
     * and artifact directory. <code>artifactDir</code> is the place where pom.xml and
     * ivy.xml are generated.
     */
    public static JkPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        final InternalPublisher ivyPublisher = IVY_CLASS_LOADER.createTransClassloaderProxy(
                InternalPublisher.class, IVY_PUB_CLASS, "of", publishRepos,
                artifactDir == null ? null : artifactDir.toFile());
        return new JkPublisher(ivyPublisher);
    }

    /**
     * Creates a {@link JkPublisher} with the specified {@link JkRepoSet}.
     */
    public static JkPublisher of(JkRepoSet publishRepos) {
        return of(publishRepos,  null);
    }

    /**
     * Publishes the specified publication to the Ivy repositories defined in
     * this publisher
     *
     * @param versionedModule
     *            The module id and version to publish
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
     *            you can mention a static version replacement. If none, you can
     *            just pass {@link JkVersionProvider#of()} }
     */
    public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                           JkDependencySet dependencies, JkScopeMapping defaultMapping,
                           Instant deliveryDate, JkVersionProvider resolvedVersion) {
        this.internalPublisher.publishIvy(versionedModule, publication, dependencies, defaultMapping,
                deliveryDate, resolvedVersion);
    }

    /**
     * Publishes the specified publication on the Maven repositories of this
     * publisher.
     *
     * @param versionedModule
     *            The target getModuleId and version for the specified publication
     * @param publication
     *            The content of the publication
     * @param dependencies
     *            The dependencies to specify in the generated pom file.
     */
    public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
            JkDependencySet dependencies) {
        assertFilesToPublishExist(publication);
        this.internalPublisher.publishMaven(versionedModule, publication, dependencies.withModulesOnly());
    }

    private void assertFilesToPublishExist(JkMavenPublication publication) {
        List<Path> missingFiles = publication.missingFiles();
        if (!missingFiles.isEmpty()) {
            throw new JkException("One or several files to publish do not exist : " + missingFiles);
        }
     }



}
