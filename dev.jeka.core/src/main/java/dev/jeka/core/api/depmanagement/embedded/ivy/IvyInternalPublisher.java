package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkArtifactPublisher;
import dev.jeka.core.api.depmanagement.publication.JkInternalPublisher;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkPomMetadata;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
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
import org.apache.ivy.util.url.URLHandlerRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class IvyInternalPublisher implements JkInternalPublisher {

    private final JkRepoSet publishRepos;

    private final Path descriptorOutputDir;

    private IvyInternalPublisher(JkRepoSet publishRepo, Path descriptorOutputDir) {
        URLHandlerRegistry.setDefault(IvyCustomUrlHandler.of(publishRepo)); // must be set before ivy is initialised
        this.publishRepos = publishRepo;
        this.descriptorOutputDir = descriptorOutputDir;
    }

    static IvyInternalPublisher of(JkRepoSet publishRepos, Path descriptorOutputDir) {
        return new IvyInternalPublisher(publishRepos, descriptorOutputDir);
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


    @Override
    public void publishIvy(JkCoordinate coordinate,
                           List<JkIvyPublication.JkIvyPublishedArtifact> publishedArtifacts,
                           JkQualifiedDependencySet dependencies) {
        JkLog.startTask( "publish-on-ivy-repos");
        final ModuleDescriptor moduleDescriptor = IvyTranslatorToModuleDescriptor.toIvyPublishModuleDescriptor(
                coordinate, dependencies, publishedArtifacts);
        final Ivy ivy = IvyTranslatorToIvy.toIvy(JkRepoSet.of(), JkResolutionParameters.of());
        int publishCount = publishIvyArtifacts(publishedArtifacts, Instant.now(), moduleDescriptor, ivy.getSettings());
        if (publishCount == 0) {
            JkLog.warn("No Ivy repository matching for " + coordinate + " found. \nConfigured repos are "
                    + publishRepos);
        }
        JkLog.endTask();
    }

    @Override
    public void publishMaven(JkCoordinate coordinate,
                             JkArtifactPublisher artifactPublisher,
                             JkPomMetadata metadata,
                             JkDependencySet dependencies,
                             Map<JkModuleId, JkVersion > managedDependencies) {
        final DefaultModuleDescriptor moduleDescriptor = createModuleDescriptorForMavenPublish(coordinate,
                artifactPublisher, dependencies);
        final Ivy ivy = IvyTranslatorToIvy.toIvy(publishRepos, JkResolutionParameters.of());
        final int count = publishMavenArtifacts(artifactPublisher, metadata, ivy.getSettings(), moduleDescriptor,
                managedDependencies);
        if (count == 0) {
            JkLog.warn("No repository has been configured for Maven publication of %s. Nothing has been published",
                    coordinate);
        } else {
            JkLog.info("Module published in %s.", JkUtilsString.pluralize(count, "repository", "repositories"));
        }
    }

    private int publishIvyArtifacts(List<JkIvyPublication.JkIvyPublishedArtifact> publishedArtifacts, Instant date,
                                    ModuleDescriptor moduleDescriptor, IvySettings ivySettings) {
        int count = 0;
        for (JkRepo publishRepo : this.publishRepos.getRepos()) {
            RepositoryResolver resolver = IvyTranslatorToResolver.convertToPublishAndBind(publishRepo, ivySettings);
            ModuleRevisionId moduleRevisionId = moduleDescriptor.getModuleRevisionId();
            final JkCoordinate coordinate = IvyTranslatorToDependency.toJkCoordinate(moduleRevisionId);
            JkVersion version = coordinate.getVersion();
            if (!isMaven(resolver) && publishRepo.publishConfig.getVersionFilter().test(version)) {
                JkLog.startTask("publish-to-repository " + resolver);
                this.publishIvyArtifacts(resolver, publishedArtifacts, date, moduleDescriptor, ivySettings);
                JkLog.endTask();
                count++;
            }
        }
        return count;
    }



    private void publishIvyArtifacts(DependencyResolver resolver,
                                     List<JkIvyPublication.JkIvyPublishedArtifact> publishedArtifacts,
                                     Instant date, ModuleDescriptor moduleDescriptor, IvySettings ivySettings) {
        final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
        try {
            resolver.beginPublishTransaction(ivyModuleRevisionId, true);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            Iterator<JkIvyPublication.JkIvyPublishedArtifact> it = publishedArtifacts.iterator();
            while (it.hasNext()) {
                JkIvyPublication.JkIvyPublishedArtifact artifact = it.next();
                final Artifact ivyArtifact = IvyTranslatorToArtifact.toIvyArtifact(artifact, ivyModuleRevisionId, date);
                try {
                    resolver.publish(ivyArtifact, artifact.file.toFile(), true);
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
        updateCache(moduleDescriptor, ivySettings);
    }

    private int publishMavenArtifacts(JkArtifactPublisher artifactPublisher,
                                      JkPomMetadata pomMetadata,
                                      IvySettings ivySettings,
                                      DefaultModuleDescriptor moduleDescriptor,
                                      Map<JkModuleId, JkVersion > managedDependencies) {
        int count = 0;
        for (JkRepo publishRepo : this.publishRepos.getRepos()) {
            RepositoryResolver resolver = IvyTranslatorToResolver.convertToPublishAndBind(publishRepo, ivySettings);
            ModuleRevisionId moduleRevisionId = moduleDescriptor.getModuleRevisionId();
            JkCoordinate coordinate = IvyTranslatorToDependency.toJkCoordinate(moduleRevisionId);
            JkVersion version = coordinate.getVersion();
            if (isMaven(resolver) && publishRepo.publishConfig.getVersionFilter().test(version)) {
                JkLog.startTask("publish-artifact-on-maven-repo");
                JkLog.info("Publish artifact %s to repo %s", coordinate, publishRepo.getUrl());
                boolean signatureRequired = publishRepo.publishConfig.isSignatureRequired();
                JkFileSigner signer = publishRepo.publishConfig.getSigner();
                if (signatureRequired && signer == null) {
                    throw new IllegalStateException("Repo " + publishRepo + " requires file signature but " +
                            "no signer has been defined on.");
                }
                IvyPublisherForMaven ivyPublisherForMaven = new IvyPublisherForMaven(
                    signer, resolver, descriptorOutputDir,
                    publishRepo.publishConfig.isUniqueSnapshot(),
                    publishRepo.publishConfig.getChecksumAlgos());
                ivyPublisherForMaven.publish(moduleDescriptor, artifactPublisher, pomMetadata, managedDependencies);
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

    private void updateCache(ModuleDescriptor moduleDescriptor, IvySettings ivySettings) {
        final ResolutionCacheManager cacheManager = ivySettings.getResolutionCacheManager();
        try {
            cacheManager.saveResolvedModuleDescriptor(moduleDescriptor);
            final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleDescriptor
                    .getModuleRevisionId());
            propsFile.delete();  //NOSONAR
        } catch (final ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultModuleDescriptor createModuleDescriptorForMavenPublish(JkCoordinate coordinate,
                                                           JkArtifactPublisher artifactPublisher,
                                                           JkDependencySet dependencies) {
        return IvyTranslatorToModuleDescriptor.toMavenPublishModuleDescriptor(coordinate, dependencies,
                artifactPublisher);
    }

    private static void commitPublication(DependencyResolver resolver) {
        try {
            resolver.commitPublishTransaction();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

}
