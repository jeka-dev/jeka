package org.jerkar.depmanagement.ivy;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
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
import org.jerkar.depmanagement.JkArtifact;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkRepos;
import org.jerkar.depmanagement.JkResolutionParameters;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkPublishRepos;
import org.jerkar.publishing.JkPublishRepos.JakePublishRepo;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsString;

/**
 * Ivy wrapper providing high level methods. The API is expressed using Jake classes only (mostly free of Ivy classes).
 * 
 * @author Jerome Angibaud
 */
public final class JkIvy {

	private static final JkVersionedModule ANONYMOUS_MODULE = JkVersionedModule.of(
			JkModuleId.of("anonymousGroup", "anonymousName"), JkVersion.named("anonymousVersion"));

	private final Ivy ivy;

	private final JkPublishRepos publishRepo;

	private JkIvy(Ivy ivy, JkPublishRepos publishRepo) {
		super();
		this.ivy = ivy;
		this.publishRepo = publishRepo;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
	}

	private static JkIvy of(IvySettings ivySettings, JkPublishRepos publishRepos) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new JkIvy(ivy, publishRepos);
	}

	/**
	 * Creates an instance using a single repository (same for resolving and publishing).
	 */
	public static JkIvy of(JkRepo repo) {
		return JkIvy.of(JkRepos.of(repo));
	}

	/**
	 * Creates an instance using specified repositories for resolving. The publishing
	 * is done on the first of the specified repository.
	 */
	public static JkIvy of(JkRepos repos) {
		return of(JkPublishRepos.of(repos.iterator().next()), repos);
	}

	/**
	 * Creates an <code>IvySettings</code> from the specified repositories.
	 */
	public static IvySettings ivySettingsOf(JkPublishRepos publishRepos, JkRepos resolveRepos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithRepo(ivySettings, resolveRepos);
		Translations.populateIvySettingsWithPublishRepo(ivySettings, publishRepos);
		return ivySettings;
	}

	/**
	 * Creates an instance using specified repository for publishing and
	 * the specified repositories for resolving.
	 */
	public static JkIvy of(JkPublishRepos publishRepos, JkRepos resolveRepos) {
		return of(ivySettingsOf(publishRepos, resolveRepos), publishRepos);
	}

	public static JkIvy of() {
		return of(JkRepos.mavenCentral());
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

	public Set<JkArtifact> resolve(JkDependencies deps, JkScope resolvedScope) {
		return resolve(ANONYMOUS_MODULE, deps, resolvedScope, JkResolutionParameters.of());
	}

	public Set<JkArtifact> resolve(JkDependencies deps, JkScope resolvedScope, JkResolutionParameters parameters) {
		return resolve(ANONYMOUS_MODULE, deps, resolvedScope, parameters);
	}



	public Set<JkArtifact> resolve(JkVersionedModule module, JkDependencies deps, JkScope resolvedScope, JkResolutionParameters parameters) {
		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(module, deps, parameters.defaultScope(), parameters.defaultMapping());

		final ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] {resolvedScope.name()});
		resolveOptions.setTransitive(true);
		resolveOptions.setOutputReport(JkOptions.isVerbose());
		resolveOptions.setLog(logLevel());
		resolveOptions.setRefresh(parameters.refreshed());
		final ResolveReport report;
		try {
			report = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (final Exception e1) {
			throw new RuntimeException(e1);
		}
		if (report.hasError()) {
			throw new JkException("Erros while resolving dependencies : " + report.getAllProblemMessages());
		}
		final Set<JkArtifact> result = new HashSet<JkArtifact>();
		final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
		for (final String conf : resolveOptions.getConfs()) {
			result.addAll(getArtifacts(conf, artifactDownloadReports));
		}
		return result;
	}

	/**
	 * Get artifacts of the given modules published for the specified scopes (no transitive resolution).
	 */
	public AttachedArtifacts getArtifacts(Iterable<JkVersionedModule> modules, JkScope ...scopes) {
		//final String defaultConf = "default";
		final DefaultModuleDescriptor moduleDescriptor = Translations.toUnpublished(ANONYMOUS_MODULE);
		for (final JkScope jakeScope : scopes) {
			moduleDescriptor.addConfiguration(new Configuration(jakeScope.name()));
		}
		for (final JkVersionedModule module : modules) {
			final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(module);
			final DefaultDependencyDescriptor dependency = new DefaultDependencyDescriptor(moduleRevisionId, true, false);
			for (final JkScope scope : scopes) {
				dependency.addDependencyConfiguration(scope.name(), scope.name());
			}
			moduleDescriptor.addDependency(dependency);
		}
		final AttachedArtifacts result = new AttachedArtifacts();
		final ResolveOptions resolveOptions = new ResolveOptions()
		.setTransitive(false)
		.setOutputReport(JkOptions.isVerbose())
		.setRefresh(false);
		resolveOptions.setLog(logLevel());
		for (final JkScope scope : scopes ) {
			resolveOptions.setConfs(Translations.toConfNames(scope));
			final ResolveReport report;
			try {
				report = ivy.resolve(moduleDescriptor, resolveOptions);
			} catch (final Exception e1) {
				throw new RuntimeException(e1);
			}
			final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
			for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
				final JkArtifact artifact = Translations.to(artifactDownloadReport.getArtifact(),
						artifactDownloadReport.getLocalFile());
				result.add(scope, artifact);
			}
		}
		return result;
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
	public void publish(JkVersionedModule versionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		JkLog.startln("Publishing for Ivy");
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
		publishIvyArtifacts(publication, deliveryDate, moduleDescriptor);
		JkLog.done();
	}



	public void publish(JkVersionedModule versionedModule, JkMavenPublication publication, JkDependencies dependencies, Date deliveryDate) {
		JkLog.startln("Publishing for Maven");
		final JkDependencies publishedDependencies = resolveDependencies(versionedModule, dependencies);
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, publishedDependencies,deliveryDate);
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
			final JakePublishRepo publishRepo = this.publishRepo.getRepoHavingUrl(Translations.publishResolverUrl(resolver));
			final JkVersionedModule jakeModule = Translations.toJakeVersionedModule(moduleDescriptor.getModuleRevisionId());
			if (!isMaven(resolver) && publishRepo.filter().accept(jakeModule)) {
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

	private int publishMavenArtifacts(JkMavenPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		int count = 0;
		for (final DependencyResolver resolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			final JakePublishRepo publishRepo = this.publishRepo.getRepoHavingUrl(Translations.publishResolverUrl(resolver));
			final JkVersionedModule jakeModule = Translations.toJakeVersionedModule(moduleDescriptor.getModuleRevisionId());
			if (isMaven(resolver) && publishRepo.filter().accept(jakeModule)) {
				JkLog.startln("Publishing for repository " + resolver);
				this.publishMavenArtifacts(resolver, publication, date, moduleDescriptor);
				JkLog.done();
				count ++;
			}
		}
		return count;
	}

	private void publishMavenArtifacts(DependencyResolver resolver, JkMavenPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
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
		try {
			final File pomXml = new File(targetDir(), "pom.xml");
			final Artifact artifact = new DefaultArtifact(ivyModuleRevisionId, date, ivyModuleRevisionId.getName(), "xml", "pom", true);

			PomModuleDescriptorWriter.write(moduleDescriptor, pomXml, new PomWriterOptions());
			resolver.publish(artifact, pomXml,	true);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
		commitOrAbortPublication(resolver);
	}

	private ModuleDescriptor createModuleDescriptor(JkVersionedModule jakeVersionedModule, JkIvyPublication publication, JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping, Date deliveryDate) {
		final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(jakeVersionedModule);

		// First : update the module ivy cache.
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);
		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(jakeVersionedModule, dependencies, defaultScope, defaultMapping);
		Translations.populateModuleDescriptorWithPublication(moduleDescriptor, publication, deliveryDate);
		try {
			cacheManager.saveResolvedModuleDescriptor(moduleDescriptor);
		} catch (final Exception e) {
			cachedIvyFile.delete();
			propsFile.delete();
			throw new RuntimeException("Error while creating cache file for " + moduleRevisionId + ". Deleting potentially corrupted cache files.", e);
		}

		// Second : update the module property cache (by invoking resolution)
		this.resolveDependencies(jakeVersionedModule, dependencies);

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

	private ModuleDescriptor createModuleDescriptor(JkVersionedModule jakeVersionedModule, JkMavenPublication publication, JkDependencies resolvedDependencies, Date deliveryDate) {
		final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(jakeVersionedModule);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);

		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(jakeVersionedModule, resolvedDependencies, null, null);
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
		return targetDir() + "/jake/[organisation]-[module]-[revision]-ivy.xml";
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

	private static Set<JkArtifact> getArtifacts(String config, ArtifactDownloadReport[] artifactDownloadReports) {
		final Set<JkArtifact> result = new HashSet<JkArtifact>();
		for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
			result.add(Translations.to(artifactDownloadReport.getArtifact(),
					artifactDownloadReport.getLocalFile()));
		}
		return result;
	}



	public final class AttachedArtifacts {

		private final Map<JkModuleId, Map<JkScope, Set<JkArtifact>>> map= new HashMap<JkModuleId, Map<JkScope,Set<JkArtifact>>>();

		public AttachedArtifacts() {
			super();
		}

		public Set<JkArtifact> getArtifacts(JkModuleId moduleId, JkScope jakeScope) {
			final Map<JkScope, Set<JkArtifact>> subMap = map.get(moduleId);
			if (subMap == null) {
				return Collections.emptySet();
			}
			final Set<JkArtifact> artifacts = subMap.get(jakeScope);
			if (artifacts == null) {
				return Collections.emptySet();
			}
			return artifacts;

		}

		public void add(JkScope scope, JkArtifact artifact) {
			Map<JkScope, Set<JkArtifact>> subMap = map.get(artifact.versionedModule().moduleId());
			if (subMap == null) {
				subMap = new HashMap<JkScope, Set<JkArtifact>>();
				map.put(artifact.versionedModule().moduleId(), subMap);
			}
			Set<JkArtifact> subArtifacts = subMap.get(scope);
			if (subArtifacts == null) {
				subArtifacts = new HashSet<JkArtifact>();
				subMap.put(scope, subArtifacts);
			}
			subArtifacts.add(artifact);
		}

		@Override
		public String toString() {
			return this.map.toString();
		}

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
