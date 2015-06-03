package org.jerkar.internal.ivy;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.resolver.AbstractPatternsBasedResolver;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.jerkar.JkException;
import org.jerkar.JkLog;
import org.jerkar.JkOptions;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkPublishRepos;
import org.jerkar.publishing.JkPublishRepos.JkPublishRepo;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsString;

/**
 * Ivy wrapper providing high level methods. The API is expressed using Jerkar classes only (mostly free of Ivy classes).
 * 
 * @author Jerome Angibaud
 */
public final class JkIvyPublisher {

	private final Ivy ivy;

	private final JkPublishRepos publishRepo;

	private JkIvyPublisher(Ivy ivy, JkPublishRepos publishRepo) {
		super();
		this.ivy = ivy;
		this.publishRepo = publishRepo;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
	}

	private static JkIvyPublisher of(IvySettings ivySettings, JkPublishRepos publishRepos) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new JkIvyPublisher(ivy, publishRepos);
	}

	/**
	 * Creates an <code>IvySettings</code> from the specified repositories.
	 */
	private static IvySettings ivySettingsOf(JkPublishRepos publishRepos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithPublishRepo(ivySettings, publishRepos);
		return ivySettings;
	}

	/**
	 * Creates an instance using specified repository for publishing and
	 * the specified repositories for resolving.
	 */
	public static JkIvyPublisher of(JkPublishRepos publishRepos) {
		return of(ivySettingsOf(publishRepos), publishRepos);
	}



	private static boolean isMaven(DependencyResolver dependencyResolver) {
		if (dependencyResolver instanceof ChainResolver) {
			final ChainResolver resolver = (ChainResolver) dependencyResolver;
			@SuppressWarnings("rawtypes")
			final List list = resolver.getResolvers();
			if (list.isEmpty()) {
				return false;
			}
			return isMaven((DependencyResolver) list.get(0));
		}
		if (dependencyResolver instanceof AbstractPatternsBasedResolver) {
			final AbstractPatternsBasedResolver resolver = (AbstractPatternsBasedResolver) dependencyResolver;
			return resolver.isM2compatible();
		}
		throw new IllegalStateException(dependencyResolver.getClass().getName() + " not handled");
	}

	public boolean hasMavenPublishRepo() {
		for (final DependencyResolver dependencyResolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			if (isMaven(dependencyResolver)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasIvyPublishRepo() {
		for (final DependencyResolver dependencyResolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			if (!isMaven(dependencyResolver)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Publish the specified module. Dependencies, default scopes and mapping are necessary
	 * in order to generate the ivy.xml file.
	 * 
	 * @param versionedModule The module/version to publish.
	 * @param publication The artifacts to publish.
	 * @param dependencies The dependencies of the published module.
	 * @param defaultScope The default scope of the published module
	 * @param defaultMapping The default scope mapping of the published module
	 * @param deliveryDate The delivery date.
	 */
	public void publishToIvyRepo(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		JkLog.startln("Publishing for Ivy");
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
		publishIvyArtifacts(publication, deliveryDate, moduleDescriptor);
		JkLog.done();
	}



	public void publishToMavenRepo(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate) {
		JkLog.startln("Publishing for Maven");
		final JkDependencies publishedDependencies = resolveDependencies(versionedModule, dependencies);
		final DefaultModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, publishedDependencies,deliveryDate);

		publishMavenArtifacts(publication, deliveryDate, moduleDescriptor);
		JkLog.done();
	}

	@SuppressWarnings("unchecked")
	private JkDependencies resolveDependencies(JkVersionedModule module, JkDependencies dependencies) {
		if (!dependencies.hasDynamicAndResovableVersions()) {
			return dependencies;
		}
		final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(module);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final File cachedPropFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);
		if (!cachedIvyFile.exists() || !cachedPropFile.exists()) {
			JkLog.start("Cached resolved ivy file not found for " + module + ". Performing a fresh resolve");
			final ModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(module, dependencies, null, null);
			final ResolveOptions resolveOptions = new ResolveOptions();
			resolveOptions.setConfs(new String[] {"*"});
			resolveOptions.setTransitive(false);
			resolveOptions.setOutputReport(JkOptions.isVerbose());
			resolveOptions.setLog(logLevel());
			resolveOptions.setRefresh(true);
			final ResolveReport report;
			try {
				report = ivy.resolve(moduleDescriptor, resolveOptions);
			} catch (final Exception e1) {
				throw new IllegalStateException(e1);
			} finally {
				JkLog.done();
			}
			if (report.hasError()) {
				JkLog.error(report.getAllProblemMessages());
				cachedIvyFile.delete();
				throw new JkException("Error while reloving dependencies : "
						+ JkUtilsString.join(report.getAllProblemMessages(), ", "));
			}
		}
		try {
			cacheManager.getResolvedModuleDescriptor(moduleRevisionId);
		} catch (final ParseException e) {
			throw new IllegalStateException(e);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
		final Properties props = JkUtilsFile.readPropertyFile(cachedPropFile);
		final Map<JkModuleId, JkVersion> resolvedVersions = Translations.toModuleVersionMap(props);
		return dependencies.resolvedWith(resolvedVersions);
	}

	private int publishIvyArtifacts(JkIvyPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		int count = 0;
		for (final DependencyResolver resolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			final JkPublishRepo publishRepo = this.publishRepo.getRepoHavingUrl(Translations.publishResolverUrl(resolver));
			final JkVersionedModule jkModule = Translations.toJerkarVersionedModule(moduleDescriptor.getModuleRevisionId());
			if (!isMaven(resolver) && publishRepo.filter().accept(jkModule)) {
				JkLog.startln("Publishing for repository " + resolver);
				this.publishIvyArtifacts(resolver, publication, date, moduleDescriptor);
				JkLog.done();;
				count++;
			}
		}
		return count;
	}

	private void publishIvyArtifacts(DependencyResolver resolver, JkIvyPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}

		for (final JkIvyPublication.Artifact artifact : publication) {
			final Artifact ivyArtifact = Translations.toPublishedArtifact(artifact, ivyModuleRevisionId, date);
			try {
				resolver.publish(ivyArtifact, artifact.file, true);
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}

		// Publish Ivy file
		final File publishedIvy = this.ivy.getSettings().resolveFile(IvyPatternHelper.substitute(ivyPatternForIvyFiles(),
				moduleDescriptor.getResolvedModuleRevisionId()));
		try {
			final Artifact artifact = MDArtifact.newIvyArtifact(moduleDescriptor);
			resolver.publish(artifact, publishedIvy, true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		try {
			commitOrAbortPublication(resolver);
		} finally {
			publishedIvy.delete();
		}
	}

	private int publishMavenArtifacts(JkMavenPublication publication, Date date, DefaultModuleDescriptor moduleDescriptor) {
		int count = 0;
		for (final DependencyResolver resolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			final JkPublishRepo publishRepo = this.publishRepo.getRepoHavingUrl(Translations.publishResolverUrl(resolver));
			final JkVersionedModule jkModule = Translations.toJerkarVersionedModule(moduleDescriptor.getModuleRevisionId());
			if (isMaven(resolver) && publishRepo.filter().accept(jkModule)) {
				JkLog.startln("Publishing for repository " + resolver);
				this.publishMavenArtifacts(resolver, publication, date, moduleDescriptor);
				JkLog.done();
				count ++;
			}
		}
		return count;
	}

	private void publishMavenArtifacts(DependencyResolver resolver, JkMavenPublication publication, Date date, DefaultModuleDescriptor moduleDescriptor) {
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final Artifact mavenMainArtifact = Translations.toPublishedMavenArtifact(publication.mainArtifactFile(), publication.artifactName(),
				null, ivyModuleRevisionId, date);
		try {
			resolver.publish(mavenMainArtifact, publication.mainArtifactFile(), true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		for (final Map.Entry<String, File> extraArtifact : publication.extraArtifacts().entrySet()) {
			final String classifier = extraArtifact.getKey();
			final File file = extraArtifact.getValue();
			final Artifact mavenArtifact = Translations.toPublishedMavenArtifact(file, publication.artifactName(),
					classifier, ivyModuleRevisionId, date);
			try {
				resolver.publish(mavenArtifact, file, true);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		//		for (final File extraFile : publication.extraFiles()) {
		//
		//			final Artifact mavenArtifact = new DefaultArtifact(ivyModuleRevisionId, date, publication.artifactName(), type, extension, extraMap);
		//
		//					Translations.toPublishedMavenArtifact(extraFile, publication.artifactName(),
		//					null, ivyModuleRevisionId, date);
		//			try {
		//				resolver.publish(mavenArtifact, extraFile, true);
		//			} catch (final IOException e) {
		//				throw new RuntimeException(e);
		//			}
		//		}
		try {
			final File pomXml = new File(targetDir(), "pom.xml");
			final Artifact artifact = new DefaultArtifact(ivyModuleRevisionId, date, publication.artifactName(), "xml", "pom", true);
			final PomWriterOptions pomWriterOptions = new PomWriterOptions();
			File fileToDelete = null;
			if (publication.extraInfo() != null) {
				final File template = PomTemplateGenerator.generateTemplate(publication.extraInfo());
				pomWriterOptions.setTemplate(template);
				fileToDelete = template;
			}

			PomModuleDescriptorWriter.write(moduleDescriptor, pomXml, pomWriterOptions);
			if (fileToDelete != null) {
				JkUtilsFile.delete(fileToDelete);
			}
			resolver.publish(artifact, pomXml,	true);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
		commitOrAbortPublication(resolver);
	}



	private ModuleDescriptor createModuleDescriptor(JkVersionedModule jkVersionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(jkVersionedModule);

		// First : update the module ivy cache.
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);
		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(jkVersionedModule, dependencies, defaultScope, defaultMapping);
		Translations.populateModuleDescriptorWithPublication(moduleDescriptor, publication, deliveryDate);
		try {
			cacheManager.saveResolvedModuleDescriptor(moduleDescriptor);
		} catch (final Exception e) {
			cachedIvyFile.delete();
			propsFile.delete();
			throw new RuntimeException("Error while creating cache file for " + moduleRevisionId + ". Deleting potentially corrupted cache files.", e);
		}

		// Second : update the module property cache (by invoking resolution)
		this.resolveDependencies(jkVersionedModule, dependencies);

		// Third : invoke the deliver process in order to generate the module ivy file.
		final DeliverOptions deliverOptions = new DeliverOptions();
		if (publication.status != null) {
			deliverOptions.setStatus(publication.status.name());
		}
		deliverOptions.setPubBranch(publication.branch);
		deliverOptions.setPubdate(deliveryDate);
		try {
			this.ivy.getDeliverEngine().deliver(moduleRevisionId, moduleRevisionId.getRevision(), ivyPatternForIvyFiles(), deliverOptions);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		return moduleDescriptor;
	}

	private DefaultModuleDescriptor createModuleDescriptor(JkVersionedModule jkVersionedModule, JkMavenPublication publication, JkDependencies resolvedDependencies, Date deliveryDate) {
		final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(jkVersionedModule);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);

		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(jkVersionedModule, resolvedDependencies, null, null);
		Translations.populateModuleDescriptorWithPublication(moduleDescriptor, publication, deliveryDate);

		try {
			cacheManager.saveResolvedModuleDescriptor(moduleDescriptor);
		} catch (final Exception e) {
			cachedIvyFile.delete();
			propsFile.delete();
			throw new RuntimeException("Error while creating cache file for "
					+ moduleRevisionId + ". Deleting potentially corrupted cache files.", e);
		}
		return moduleDescriptor;
	}

	private String ivyPatternForIvyFiles() {
		return targetDir() + "/jerkar/[organisation]-[module]-[revision]-ivy.xml";
	}

	private String targetDir() {
		return System.getProperty("java.io.tmpdir");
	}

	private static String logLevel() {
		if (JkOptions.isSilent()) {
			return "quiet";
		}
		if (JkOptions.isVerbose()) {
			return "default";
		}
		return "download-only";
	}


	private static void commitOrAbortPublication(DependencyResolver resolver) {
		try {
			resolver.commitPublishTransaction();
		} catch (final Exception e) {
			try {
				resolver.abortPublishTransaction();
			} catch (final IOException e1) {
				throw new RuntimeException(e);
			}
		}
	}





}
