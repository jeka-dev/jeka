package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

final class IvyInternalPublisher implements JkInternalPublisher {

    private final JkRepoSet publishRepos;

    private final Path descriptorOutputDir;

    private IvyInternalPublisher(JkRepoSet publishRepo, Path descriptorOutputDir) {
        super();
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
    public void publishIvy(JkVersionedModule versionedModule,
                           List<JkIvyPublication.JkPublishedArtifact> publishedArtifacts,
                           JkQualifiedDependencies dependencies) {
        JkLog.startTask( "Publish on Ivy repositories");
        final ModuleDescriptor moduleDescriptor = IvyTranslatorToModuleDescriptor.toIvyPublishModuleDescriptor(
                versionedModule, dependencies, publishedArtifacts);
        final Ivy ivy = IvyTranslatorToIvy.toIvy(JkRepoSet.of(), JkResolutionParameters.of());
        int publishCount = publishIvyArtifacts(publishedArtifacts, Instant.now(), moduleDescriptor, ivy.getSettings());
        if (publishCount == 0) {
            JkLog.warn("No Ivy repository matching for " + versionedModule + " found. Configured repos are "
                    + publishRepos);
        }
        JkLog.endTask();
    }

    @Override
    public void publishMaven(JkVersionedModule versionedModule, JkArtifactLocator artifactLocator,
                             JkPomMetadata metadata, JkDependencySet dependencies) {
        JkLog.startTask("Publish on Maven repositories");
        final DefaultModuleDescriptor moduleDescriptor = createModuleDescriptorForMavenPublish(versionedModule,
                artifactLocator, dependencies);
        final Ivy ivy = IvyTranslatorToIvy.toIvy(publishRepos, JkResolutionParameters.of());
        final int count = publishMavenArtifacts(artifactLocator, metadata, ivy.getSettings(), moduleDescriptor);
        JkLog.info("Module published in %s.", JkUtilsString.plurialize(count, "repository", "repositories"));
        JkLog.endTask();
    }

    private int publishIvyArtifacts(List<JkIvyPublication.JkPublishedArtifact> publishedArtifacts, Instant date,
                                    ModuleDescriptor moduleDescriptor, IvySettings ivySettings) {
        int count = 0;
        for (JkRepo publishRepo : this.publishRepos.getRepos()) {
            RepositoryResolver resolver = IvyTranslatorToResolver.convertToPublishAndBind(publishRepo, ivySettings);
            ModuleRevisionId moduleRevisionId = moduleDescriptor.getModuleRevisionId();
            final JkVersionedModule versionedModule = IvyTranslatorToDependency.toJkVersionedModule(moduleRevisionId);
            JkVersion version = versionedModule.getVersion();
            if (!isMaven(resolver) && publishRepo.getPublishConfig().getVersionFilter().test(version)) {
                JkLog.startTask("Publish for repository " + resolver);
                this.publishIvyArtifacts(resolver, publishedArtifacts, date, moduleDescriptor, ivySettings);
                JkLog.endTask();
                count++;
            }
        }
        return count;
    }



    private void publishIvyArtifacts(DependencyResolver resolver,
                                     List<JkIvyPublication.JkPublishedArtifact> publishedArtifacts,
                                     Instant date, ModuleDescriptor moduleDescriptor, IvySettings ivySettings) {
        final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
        try {
            resolver.beginPublishTransaction(ivyModuleRevisionId, true);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            Iterator<JkIvyPublication.JkPublishedArtifact> it = publishedArtifacts.iterator();
            while (it.hasNext()) {
                JkIvyPublication.JkPublishedArtifact artifact = it.next();
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

    private int publishMavenArtifacts(JkArtifactLocator artifactLocator, JkPomMetadata pomMetadata,
                                      IvySettings ivySettings, DefaultModuleDescriptor moduleDescriptor) {
        int count = 0;
        for (JkRepo publishRepo : this.publishRepos.getRepos()) {
            RepositoryResolver resolver = IvyTranslatorToResolver.convertToPublishAndBind(publishRepo, ivySettings);
            ModuleRevisionId moduleRevisionId = moduleDescriptor.getModuleRevisionId();
            JkVersionedModule versionedModule = IvyTranslatorToDependency.toJkVersionedModule(moduleRevisionId);
            JkVersion version = versionedModule.getVersion();
            if (isMaven(resolver) && publishRepo.getPublishConfig().getVersionFilter().test(version)) {
                JkLog.startTask("Publish to " + publishRepo.getUrl());
                boolean signatureRequired = publishRepo.getPublishConfig().isSignatureRequired();
                UnaryOperator<Path> signer = publishRepo.getPublishConfig().getSigner();
                if (signatureRequired && signer == null) {
                    throw new IllegalStateException("Repo " + publishRepo + " requires file signature but " +
                            "no siigner has been defined on.");
                }
                IvyPublisherForMaven ivyPublisherForMaven = new IvyPublisherForMaven(
                    signer, resolver, descriptorOutputDir,
                    publishRepo.getPublishConfig().isUniqueSnapshot(),
                    publishRepo.getPublishConfig().getChecksumAlgos());
                ivyPublisherForMaven.publish(moduleDescriptor, artifactLocator, pomMetadata);
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
            propsFile.delete();
        } catch (final ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultModuleDescriptor createModuleDescriptorForMavenPublish(JkVersionedModule versionedModule,
                                                           JkArtifactLocator artifactLocator,
                                                           JkDependencySet dependencies) {
        return IvyTranslatorToModuleDescriptor.toMavenPublishModuleDescriptor(versionedModule, dependencies,
                artifactLocator);
    }

    private static void commitPublication(DependencyResolver resolver) {
        try {
            resolver.commitPublishTransaction();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

}
