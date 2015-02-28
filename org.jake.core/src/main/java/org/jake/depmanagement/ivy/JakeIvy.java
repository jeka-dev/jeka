package org.jake.depmanagement.ivy;

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
import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.publishing.JakeIvyPublication;
import org.jake.publishing.JakeMavenPublication;
import org.jake.publishing.JakePublishRepos;
import org.jake.publishing.JakePublishRepos.JakePublishRepo;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsString;

/**
 * Ivy wrapper providing high level methods. The API is expressed using Jake classes only (mostly free of Ivy classes).
 * 
 * @author Jerome Angibaud
 */
public final class JakeIvy {

	private static final JakeVersionedModule ANONYMOUS_MODULE = JakeVersionedModule.of(
			JakeModuleId.of("anonymousGroup", "anonymousName"), JakeVersion.named("anonymousVersion"));

	private final Ivy ivy;

	private final JakePublishRepos publishRepo;

	private JakeIvy(Ivy ivy, JakePublishRepos publishRepo) {
		super();
		this.ivy = ivy;
		this.publishRepo = publishRepo;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
	}

	private static JakeIvy of(IvySettings ivySettings, JakePublishRepos publishRepos) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new JakeIvy(ivy, publishRepos);
	}

	/**
	 * Creates an instance using a single repository (same for resolving and publishing).
	 */
	public static JakeIvy of(JakeRepo repo) {
		return JakeIvy.of(JakeRepos.of(repo));
	}

	/**
	 * Creates an instance using specified repositories for resolving. The publishing
	 * is done on the first of the specified repository.
	 */
	public static JakeIvy of(JakeRepos repos) {
		return of(JakePublishRepos.of(repos.iterator().next()), repos);
	}

	/**
	 * Creates an <code>IvySettings</code> from the specified repositories.
	 */
	public static IvySettings ivySettingsOf(JakePublishRepos publishRepos, JakeRepos resolveRepos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithRepo(ivySettings, resolveRepos);
		Translations.populateIvySettingsWithPublishRepo(ivySettings, publishRepos);
		return ivySettings;
	}

	/**
	 * Creates an instance using specified repository for publishing and
	 * the specified repositories for resolving.
	 */
	public static JakeIvy of(JakePublishRepos publishRepos, JakeRepos resolveRepos) {
		return of(ivySettingsOf(publishRepos, resolveRepos), publishRepos);
	}

	public static JakeIvy of() {
		return of(JakeRepos.mavenCentral());
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

	public Set<JakeArtifact> resolve(JakeDependencies deps, JakeScope resolvedScope) {
		return resolve(ANONYMOUS_MODULE, deps, resolvedScope, JakeResolutionParameters.of());
	}

	public Set<JakeArtifact> resolve(JakeDependencies deps, JakeScope resolvedScope, JakeResolutionParameters parameters) {
		return resolve(ANONYMOUS_MODULE, deps, resolvedScope, parameters);
	}



	public Set<JakeArtifact> resolve(JakeVersionedModule module, JakeDependencies deps, JakeScope resolvedScope, JakeResolutionParameters parameters) {
		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(module, deps, parameters.defaultScope(), parameters.defaultMapping());

		final ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] {resolvedScope.name()});
		resolveOptions.setTransitive(true);
		resolveOptions.setOutputReport(JakeOptions.isVerbose());
		resolveOptions.setLog(logLevel());
		resolveOptions.setRefresh(parameters.refreshed());
		final ResolveReport report;
		try {
			report = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (final Exception e1) {
			throw new RuntimeException(e1);
		}
		if (report.hasError()) {
			throw new JakeException("Erros while resolving dependencies : " + report.getAllProblemMessages());
		}
		final Set<JakeArtifact> result = new HashSet<JakeArtifact>();
		final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
		for (final String conf : resolveOptions.getConfs()) {
			result.addAll(getArtifacts(conf, artifactDownloadReports));
		}
		return result;
	}

	/**
	 * Get artifacts of the given modules published for the specified scopes (no transitive resolution).
	 */
	public AttachedArtifacts getArtifacts(Iterable<JakeVersionedModule> modules, JakeScope ...scopes) {
		//final String defaultConf = "default";
		final DefaultModuleDescriptor moduleDescriptor = Translations.toUnpublished(ANONYMOUS_MODULE);
		for (final JakeScope jakeScope : scopes) {
			moduleDescriptor.addConfiguration(new Configuration(jakeScope.name()));
		}
		for (final JakeVersionedModule module : modules) {
			final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(module);
			final DefaultDependencyDescriptor dependency = new DefaultDependencyDescriptor(moduleRevisionId, true, false);
			for (final JakeScope scope : scopes) {
				dependency.addDependencyConfiguration(scope.name(), scope.name());
			}
			moduleDescriptor.addDependency(dependency);
		}
		final AttachedArtifacts result = new AttachedArtifacts();
		final ResolveOptions resolveOptions = new ResolveOptions()
		.setTransitive(false)
		.setOutputReport(JakeOptions.isVerbose())
		.setRefresh(false);
		resolveOptions.setLog(logLevel());
		for (final JakeScope scope : scopes ) {
			resolveOptions.setConfs(Translations.toConfNames(scope));
			final ResolveReport report;
			try {
				report = ivy.resolve(moduleDescriptor, resolveOptions);
			} catch (final Exception e1) {
				throw new RuntimeException(e1);
			}
			final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
			for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
				final JakeArtifact artifact = Translations.to(artifactDownloadReport.getArtifact(),
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
	public void publish(JakeVersionedModule versionedModule, JakeIvyPublication publication, JakeDependencies dependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping, Date deliveryDate) {
		JakeLog.startln("Publishing for Ivy");
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, dependencies, defaultScope, defaultMapping, deliveryDate);
		publishIvyArtifacts(publication, deliveryDate, moduleDescriptor);
		JakeLog.done();
	}



	public void publish(JakeVersionedModule versionedModule, JakeMavenPublication publication, JakeDependencies dependencies, Date deliveryDate) {
		JakeLog.startln("Publishing for Maven");
		final JakeDependencies publishedDependencies = resolveDependencies(versionedModule, dependencies);
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, publishedDependencies,deliveryDate);
		publishMavenArtifacts(publication, deliveryDate, moduleDescriptor);
		JakeLog.done();
	}

	@SuppressWarnings("unchecked")
	private JakeDependencies resolveDependencies(JakeVersionedModule module, JakeDependencies dependencies) {
		if (!dependencies.hasDynamicAndResovableVersions()) {
			return dependencies;
		}
		final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(module);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final File cachedPropFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);
		if (!cachedIvyFile.exists() || !cachedPropFile.exists()) {
			JakeLog.start("Cached resolved ivy file not found for " + module + ". Performing a fresh resolve");
			final ModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(module, dependencies, null, null);
			final ResolveOptions resolveOptions = new ResolveOptions();
			resolveOptions.setConfs(new String[] {"*"});
			resolveOptions.setTransitive(false);
			resolveOptions.setOutputReport(JakeOptions.isVerbose());
			resolveOptions.setLog(logLevel());
			resolveOptions.setRefresh(true);
			final ResolveReport report;
			try {
				report = ivy.resolve(moduleDescriptor, resolveOptions);
			} catch (final Exception e1) {
				throw new IllegalStateException(e1);
			} finally {
				JakeLog.done();
			}
			if (report.hasError()) {
				JakeLog.error(report.getAllProblemMessages());
				cachedIvyFile.delete();
				throw new JakeException("Error while reloving dependencies : "
						+ JakeUtilsString.join(report.getAllProblemMessages(), ", "));
			}
		}
		try {
			cacheManager.getResolvedModuleDescriptor(moduleRevisionId);
		} catch (final ParseException e) {
			throw new IllegalStateException(e);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
		final Properties props = JakeUtilsFile.readPropertyFile(cachedPropFile);
		final Map<JakeModuleId, JakeVersion> resolvedVersions = Translations.toModuleVersionMap(props);
		return dependencies.resolvedWith(resolvedVersions);
	}

	private int publishIvyArtifacts(JakeIvyPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		int count = 0;
		for (final DependencyResolver resolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			final JakePublishRepo publishRepo = this.publishRepo.getRepoHavingUrl(Translations.publishResolverUrl(resolver));
			final JakeVersionedModule jakeModule = Translations.toJakeVersionedModule(moduleDescriptor.getModuleRevisionId());
			if (!isMaven(resolver) && publishRepo.filter().accept(jakeModule)) {
				JakeLog.startln("Publishing for repository " + resolver);
				this.publishIvyArtifacts(resolver, publication, date, moduleDescriptor);
				JakeLog.done();;
				count++;
			}
		}
		return count;
	}

	private void publishIvyArtifacts(DependencyResolver resolver, JakeIvyPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}

		for (final JakeIvyPublication.Artifact artifact : publication) {
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

	private int publishMavenArtifacts(JakeMavenPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		int count = 0;
		for (final DependencyResolver resolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			final JakePublishRepo publishRepo = this.publishRepo.getRepoHavingUrl(Translations.publishResolverUrl(resolver));
			final JakeVersionedModule jakeModule = Translations.toJakeVersionedModule(moduleDescriptor.getModuleRevisionId());
			if (isMaven(resolver) && publishRepo.filter().accept(jakeModule)) {
				JakeLog.startln("Publishing for repository " + resolver);
				this.publishMavenArtifacts(resolver, publication, date, moduleDescriptor);
				JakeLog.done();
				count ++;
			}
		}
		return count;
	}

	private void publishMavenArtifacts(DependencyResolver resolver, JakeMavenPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
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

	private ModuleDescriptor createModuleDescriptor(JakeVersionedModule jakeVersionedModule, JakeIvyPublication publication, JakeDependencies dependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping, Date deliveryDate) {
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

	private ModuleDescriptor createModuleDescriptor(JakeVersionedModule jakeVersionedModule, JakeMavenPublication publication, JakeDependencies resolvedDependencies, Date deliveryDate) {
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
		if (JakeOptions.isSilent()) {
			return "quiet";
		}
		if (JakeOptions.isVerbose()) {
			return "default";
		}
		return "download-only";
	}

	private static Set<JakeArtifact> getArtifacts(String config, ArtifactDownloadReport[] artifactDownloadReports) {
		final Set<JakeArtifact> result = new HashSet<JakeArtifact>();
		for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
			result.add(Translations.to(artifactDownloadReport.getArtifact(),
					artifactDownloadReport.getLocalFile()));
		}
		return result;
	}



	public final class AttachedArtifacts {

		private final Map<JakeModuleId, Map<JakeScope, Set<JakeArtifact>>> map= new HashMap<JakeModuleId, Map<JakeScope,Set<JakeArtifact>>>();

		public AttachedArtifacts() {
			super();
		}

		public Set<JakeArtifact> getArtifacts(JakeModuleId moduleId, JakeScope jakeScope) {
			final Map<JakeScope, Set<JakeArtifact>> subMap = map.get(moduleId);
			if (subMap == null) {
				return Collections.emptySet();
			}
			final Set<JakeArtifact> artifacts = subMap.get(jakeScope);
			if (artifacts == null) {
				return Collections.emptySet();
			}
			return artifacts;

		}

		public void add(JakeScope scope, JakeArtifact artifact) {
			Map<JakeScope, Set<JakeArtifact>> subMap = map.get(artifact.versionedModule().moduleId());
			if (subMap == null) {
				subMap = new HashMap<JakeScope, Set<JakeArtifact>>();
				map.put(artifact.versionedModule().moduleId(), subMap);
			}
			Set<JakeArtifact> subArtifacts = subMap.get(scope);
			if (subArtifacts == null) {
				subArtifacts = new HashSet<JakeArtifact>();
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
