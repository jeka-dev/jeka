package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.AbstractPatternsBasedResolver;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.RepositoryResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Jeka users : This class is not part of the public API !!! Please, Use
 * {@link JkPublisher} instead. Ivy wrapper providing high level methods. The
 * API is expressed using Jeka classes only (mostly free of Ivy classes).
 */
final class IvyInternalPublisher implements JkInternalPublisher {

    private final Ivy ivy;

    private final JkRepoSet publishRepos;

    private final Path descriptorOutputDir;

    private IvyInternalPublisher(Ivy ivy, JkRepoSet publishRepo, Path descriptorOutputDir) {
        super();
        this.ivy = ivy;
        this.publishRepos = publishRepo;
        this.descriptorOutputDir = descriptorOutputDir;
    }

    /**
     * Creates an <code>IvySettings</code> to the specified repositories.
     */
    private static IvySettings ivySettingsOf(JkRepoSet publishRepos) {
        final IvySettings ivySettings = new IvySettings();
        IvyTranslations.populateIvySettingsWithPublishRepo(ivySettings, publishRepos);
        return ivySettings;
    }

    private static IvyInternalPublisher of(IvySettings ivySettings, JkRepoSet publishRepos,
                                           Path descriptorOutputDir) {
        final Ivy ivy = IvyInternalDepResolver.ivy(ivySettings);
        return new IvyInternalPublisher(ivy, publishRepos, descriptorOutputDir);
    }

    /**
     * Creates an instance using specified repository for publishing and the
     * specified repositories for resolving.
     */
    public static IvyInternalPublisher of(JkRepoSet publishRepos, Path descriptorOutputDir) {
        return of(ivySettingsOf(publishRepos), publishRepos, descriptorOutputDir);
    }

    private static boolean isMaven(DependencyResolver dependencyResolver) {
        if (dependencyResolver instanceof ChainResolver) {
            final ChainResolver resolver = (ChainResolver) dependencyResolver;
            @SuppressWarnings("rawtypes")
            final List<DependencyResolver> list = resolver.getResolvers();
            if (list.isEmpty()) {
                return false;
            }
            return isMaven(list.get(0));
        }
        if (dependencyResolver instanceof AbstractPatternsBasedResolver) {
            final AbstractPatternsBasedResolver resolver = (AbstractPatternsBasedResolver) dependencyResolver;
            return resolver.isM2compatible();
        }
        throw new IllegalStateException(dependencyResolver.getClass().getName() + " not handled");
    }

    /**
     * Publish the specified module. Dependencies, default scopes and mapping
     * are necessary in order to generate the ivy.xml file.
     *
     * @param versionedModule
     *            The module/version to publish.
     * @param publication
     *            The artifacts to publish.
     * @param dependencies
     *            The dependencies of the published module.
     * @param defaultMapping
     *            The default scope mapping of the published module
     * @param deliveryDate
     *            The delivery date.
     */
    @Override
    public void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                           JkDependencySet dependencies, JkScopeMapping defaultMapping, Instant deliveryDate,
                           JkVersionProvider resolvedVersions) {
        JkLog.startTask( "Publishing on Ivy repositories");
        final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule,
            publication, dependencies, defaultMapping, deliveryDate, resolvedVersions);
        int publishCount = publishIvyArtifacts(publication, deliveryDate, moduleDescriptor);
        if (publishCount == 0) {
            JkLog.warn("No Ivy repository matching for " + versionedModule + " found. Configured repos are " + publishRepos);
        }
        JkLog.endTask();
    }

    @Override
    public void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
                             JkDependencySet dependencies, UnaryOperator<Path> signer) {
        JkLog.startTask("Publishing on Maven repositories");
        final DefaultModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule,
                publication, dependencies, Instant.now(), JkVersionProvider.of());
        final int count = publishMavenArtifacts(publication, moduleDescriptor, signer);
        JkLog.info("Module published in %s.", JkUtilsString.plurialize(count,
                "repository", "repositories"));
       JkLog.endTask();
    }

    private int publishIvyArtifacts(JkIvyPublication publication, Instant date,
            ModuleDescriptor moduleDescriptor) {
        int count = 0;
        for (final DependencyResolver resolver : IvyTranslations.publishResolverOf(this.ivy
                .getSettings())) {
            final JkRepo publishRepo = this.publishRepos.getRepoConfigHavingUrl(IvyTranslations
                    .publishResolverUrl(resolver));
            final JkVersionedModule versionedModule = IvyTranslations
                    .toJkVersionedModule(moduleDescriptor.getModuleRevisionId());
            if (!isMaven(resolver) && publishRepo.getPublishConfig().getVersionFilter().test(versionedModule.getVersion())) {
                JkLog.startTask("Publishing for repository " + resolver);
                this.publishIvyArtifacts(resolver, publication, date, moduleDescriptor);
                JkLog.endTask();
                count++;
            }
        }
        return count;
    }

    private void publishIvyArtifacts(DependencyResolver resolver, JkIvyPublication publication,
            Instant date, ModuleDescriptor moduleDescriptor) {
        final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
        try {
            resolver.beginPublishTransaction(ivyModuleRevisionId, true);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            Iterator<JkIvyPublication.JkPublicationArtifact> it = publication.getAllArtifacts().iterator();
            while (it.hasNext()) {
                JkIvyPublication.JkPublicationArtifact artifact = it.next();
                final Artifact ivyArtifact = IvyTranslations.toPublishedArtifact(artifact, ivyModuleRevisionId, date);
                try {
                    resolver.publish(ivyArtifact, artifact.file, true);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            // Publish Ivy file
            final Path publishedIvy = createIvyFile(moduleDescriptor);
            final Artifact artifact = MDArtifact.newIvyArtifact(moduleDescriptor);
            resolver.publish(artifact, publishedIvy.toFile(), true);
            if (this.descriptorOutputDir == null) {
                JkUtilsPath.deleteFile(publishedIvy);
            }
        } catch (final Exception e) {
            abortPublishTransaction(resolver);
            throw JkUtilsThrowable.unchecked(e);
        }
        commitPublication(resolver);
        updateCache(moduleDescriptor);
    }

    private int publishMavenArtifacts(JkMavenPublication publication,
                                      DefaultModuleDescriptor moduleDescriptor, UnaryOperator<Path> signer) {
        int count = 0;
        for (final RepositoryResolver resolver : IvyTranslations.publishResolverOf(this.ivy.getSettings())) {
            final JkRepo publishRepo = this.publishRepos.getRepoConfigHavingUrl(IvyTranslations
                    .publishResolverUrl(resolver));
            final JkVersionedModule versionedModule = IvyTranslations
                    .toJkVersionedModule(moduleDescriptor.getModuleRevisionId());
            if (isMaven(resolver) && publishRepo.getPublishConfig().getVersionFilter().test(versionedModule.getVersion())) {
                JkLog.startTask("Publishing to " + resolver);
                UnaryOperator<Path> effectiveSigner = publishRepo.getPublishConfig().isSignatureRequired() ? signer :
                        null;
                final IvyPublisherForMaven ivyPublisherForMaven = new IvyPublisherForMaven(
                    effectiveSigner, resolver, descriptorOutputDir,
                    publishRepo.getPublishConfig().isUniqueSnapshot(),
                    publishRepo.getPublishConfig().getChecksumAlgos());
                ivyPublisherForMaven.publish(moduleDescriptor, publication);
                count++;
                JkLog.endTask();
            }
        }
        return count;
    }

    private static void abortPublishTransaction(DependencyResolver resolver) {
        try {
            resolver.abortPublishTransaction();
        } catch (final IOException e) {
            JkLog.warn("Publish transaction hasn't been properly aborted");
        }
    }

    private ModuleDescriptor createModuleDescriptor(JkVersionedModule jkVersionedModule,
            JkIvyPublication publication, JkDependencySet dependencies,
            JkScopeMapping defaultMapping, Instant deliveryDate, JkVersionProvider resolvedVersions) {

        final DefaultModuleDescriptor moduleDescriptor = IvyTranslations.toPublicationLessModule(
                jkVersionedModule, dependencies, defaultMapping, resolvedVersions);
        IvyTranslations.populateModuleDescriptorWithPublication(moduleDescriptor, publication,
                deliveryDate);
        return moduleDescriptor;
    }

    private Path createIvyFile(ModuleDescriptor moduleDescriptor) {
        try {
            final ModuleRevisionId mrId = moduleDescriptor.getModuleRevisionId();
            final Path file;
            if (this.descriptorOutputDir != null) {
                file = this.descriptorOutputDir.resolve("published-of-" + mrId.getOrganisation()
                + "-" + mrId.getName() + "-" + mrId.getRevision() + ".xml");
            } else {
                file = JkUtilsPath.createTempFile("published-of-", ".xml");
            }
            moduleDescriptor.toIvyFile(file.toFile());
            return file;
        } catch (final IOException | ParseException e) {
            throw new RuntimeException(e);
        }

    }

    private void updateCache(ModuleDescriptor moduleDescriptor) {
        final ResolutionCacheManager cacheManager = this.ivy.getSettings()
                .getResolutionCacheManager();
        try {
            cacheManager.saveResolvedModuleDescriptor(moduleDescriptor);
            final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleDescriptor
                    .getModuleRevisionId());
            propsFile.delete();
        } catch (final ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultModuleDescriptor createModuleDescriptor(JkVersionedModule jkVersionedModule,
                                                           JkMavenPublication publication,
                                                           JkDependencySet resolvedDependencies,
                                                           Instant deliveryDate,
                                                           JkVersionProvider resolvedVersions) {
        final DefaultModuleDescriptor moduleDescriptor = IvyTranslations.toPublicationLessModule(
                jkVersionedModule, resolvedDependencies, JkScope.DEFAULT_SCOPE_MAPPING, resolvedVersions);
        IvyTranslations.populateModuleDescriptorWithPublication(moduleDescriptor, publication,
                deliveryDate);
        return moduleDescriptor;
    }

    private static void commitPublication(DependencyResolver resolver) {
        try {
            resolver.commitPublishTransaction();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

}
