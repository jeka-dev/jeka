package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkException;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A class to publish artifacts on repositories. According the nature of the
 * repository (Maven or Ivy) the publisher will also create the necessary
 * metadata (pom.xml, metadata.xml, ivy.xml, checksums, ...).
 *
 * @author Jerome Angibaud
 */
public final class JkPublisher {

    private static final String IVY_PUB_CLASS = "dev.jeka.core.api.depmanagement.IvyInternalPublisher";

    private final InternalPublisher internalPublisher;

    private final UnaryOperator<Path> signer;

    private JkPublisher(InternalPublisher internalPublisher, UnaryOperator<Path> signer) {
        super();
        this.internalPublisher = internalPublisher;
        this.signer = signer;
    }

    /**
     * Creates a {@link JkPublisher} with the specified {@link JkRepo}.
     */
    public static JkPublisher of(JkRepo repo) {
        return of(JkRepoSet.of(repo));
    }

    /**
     * Creates a {@link JkPublisher} with the specified <code>JkRepoSet</code>.
     * and artifact directory. <code>artifactDir</code> is the place where pom.xml and
     * ivy.xml are generated.
     */
    public static JkPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        File arg = artifactDir == null ? null : artifactDir.toFile();
        final InternalPublisher ivyPublisher;
        if (JkClassLoader.ofCurrent().isDefined(IvyClassloader.IVY_CLASS_NAME)) {
            ivyPublisher = IvyInternalPublisher.of(publishRepos, arg);
        } else {
            ivyPublisher =IvyClassloader.CLASSLOADER.createCrossClassloaderProxy(
                    InternalPublisher.class, IVY_PUB_CLASS, "of", publishRepos,
                    artifactDir == null ? null : artifactDir.toFile());
        }
        return new JkPublisher(ivyPublisher, null);
    }

    /**
     * Creates a {@link JkPublisher} with the specified <code>JkRepoSet</code>.
     */
    public static JkPublisher of(JkRepoSet publishRepos) {
        return of(publishRepos,  null);
    }

    public JkPublisher withSigner(UnaryOperator<Path> signer) {
        return new JkPublisher(internalPublisher, signer);
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
        this.internalPublisher.publishMaven(versionedModule, publication, dependencies.withModulesOnly(), this.signer);
    }

    private void assertFilesToPublishExist(JkMavenPublication publication) {
        List<Path> missingFiles = publication.missingFiles();
        if (!missingFiles.isEmpty()) {
            throw new JkException("One or several files to publish do not exist : " + missingFiles);
        }
     }

}
