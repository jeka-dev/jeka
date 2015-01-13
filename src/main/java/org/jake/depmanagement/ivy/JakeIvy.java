package org.jake.depmanagement.ivy;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.jake.utils.JakeUtilsString;

public final class JakeIvy {

	private static final JakeVersionedModule ANONYMOUS_MODULE = JakeVersionedModule.of(
			JakeModuleId.of("anonymousGroup", "anonymousName"), JakeVersion.of("anonymousVersion"));

	private final Ivy ivy;

	private JakeIvy(Ivy ivy) {
		super();
		this.ivy = ivy;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
	}

	public static JakeIvy of(IvySettings ivySettings) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new JakeIvy(ivy);
	}

	public static JakeIvy of(JakeRepo repo) {
		return JakeIvy.of(JakeRepos.of(repo));
	}

	public static JakeIvy of(JakeRepos repos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithRepo(ivySettings, repos);
		return of(ivySettings);
	}

	public static JakeIvy of() {
		return of(JakeRepos.mavenCentral());
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
			final ModuleRevisionId moduleRevisionId = Translations.from(module);
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
		final JakeDependencies publishedDependencies = resolveDependencies(versionedModule, dependencies);
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, publishedDependencies, defaultScope, defaultMapping, deliveryDate);
		publishArtifacts(publication, deliveryDate, moduleDescriptor);
	}

	public void publish(JakeVersionedModule versionedModule, JakeMavenPublication publication, JakeDependencies dependencies, Date deliveryDate) {
		final JakeDependencies publishedDependencies = resolveDependencies(versionedModule, dependencies);
		final ModuleDescriptor moduleDescriptor = createModuleDescriptor(versionedModule, publication, publishedDependencies,deliveryDate);
		publishArtifacts(publication, deliveryDate, moduleDescriptor);
	}



	@SuppressWarnings("unchecked")
	private JakeDependencies resolveDependencies(JakeVersionedModule module, JakeDependencies dependencies) {
		if (!dependencies.hasDynamicAndResovableVersions()) {
			return dependencies;
		}
		final ModuleRevisionId moduleRevisionId = Translations.from(module);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		if (!cachedIvyFile.exists()) {
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
		return dependencies;
	}

	private void publishArtifacts(JakeIvyPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		final DependencyResolver resolver = this.ivy.getSettings().getDefaultResolver();
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
		final File publishedIvy = this.ivy.getSettings().resolveFile(IvyPatternHelper.substitute(ivyPatternForIvyFiles(),
				moduleDescriptor.getResolvedModuleRevisionId()));
		try {
			resolver.publish(createIvyModuleDescriptorArtifact(moduleDescriptor), publishedIvy,
					true);
			resolver.commitPublishTransaction();
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		} finally {
			publishedIvy.delete();
		}
	}

	private void publishArtifacts(JakeMavenPublication publication, Date date, ModuleDescriptor moduleDescriptor) {
		final DependencyResolver resolver = this.ivy.getSettings().getDefaultResolver();
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}

		for (final JakeMavenPublication.Artifact artifact : publication) {
			final Artifact mavenArtifact = Translations.toPublishedArtifact(artifact, ivyModuleRevisionId, date);
			try {
				resolver.publish(mavenArtifact, artifact.file(), true);
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}
		final File pomFile = new File(targetDir(), "pom.xml");
		createPomFile(moduleDescriptor, pomFile);
		final Artifact artifact = new DefaultArtifact();
		resolver.publish(artifact, src, overwrite);
		pomFile.delete();
	}

	private ModuleDescriptor createModuleDescriptor(JakeVersionedModule jakeVersionedModule, JakeIvyPublication publication, JakeDependencies resolvedDependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping, Date deliveryDate) {
		final ModuleRevisionId moduleRevisionId = Translations.from(jakeVersionedModule);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final String propsfileName = JakeUtilsString.substringBeforeLast(cachedIvyFile.getName(), ".") + ".properties";
		final File propsFile = new File(cachedIvyFile.getParent(), propsfileName);
		if (!propsFile.exists()) {
			try {
				propsFile.createNewFile();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(jakeVersionedModule, resolvedDependencies, defaultScope, defaultMapping);
		Translations.populateModuleDescriptorWithPublication(moduleDescriptor, publication, deliveryDate);
		try {
			cacheManager.saveResolvedModuleDescriptor(moduleDescriptor);
		} catch (final Exception e) {
			cachedIvyFile.delete();
			propsFile.delete();
			throw new RuntimeException("Error while creating cache file for " + moduleRevisionId + ". Deleting potentially corrupted cache files.", e);
		}

		final DeliverOptions deliverOptions = new DeliverOptions();
		if (publication.status != null) {
			deliverOptions.setStatus(publication.status.name());
		}
		deliverOptions.setPubBranch(publication.branch);
		deliverOptions.setPubdate(deliveryDate);

		try {
			this.ivy.getDeliverEngine().deliver(Translations.from(jakeVersionedModule), jakeVersionedModule.version().name(), ivyPatternForIvyFiles(), deliverOptions);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		} catch (final ParseException e) {
			throw new IllegalStateException(e);
		}

		return moduleDescriptor;
	}

	private ModuleDescriptor createModuleDescriptor(JakeVersionedModule jakeVersionedModule, JakeMavenPublication publication, JakeDependencies resolvedDependencies, Date deliveryDate) {
		final ModuleRevisionId moduleRevisionId = Translations.from(jakeVersionedModule);
		final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
		final File cachedIvyFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
		final String propsfileName = JakeUtilsString.substringBeforeLast(cachedIvyFile.getName(), ".") + ".properties";
		final File propsFile = new File(cachedIvyFile.getParent(), propsfileName);
		if (!propsFile.exists()) {
			try {
				propsFile.createNewFile();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
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

	private static Artifact createIvyModuleDescriptorArtifact(ModuleDescriptor descriptor) {
		return MDArtifact.newIvyArtifact(descriptor);
	}

	private static void createPomFile(ModuleDescriptor descriptor, File file) {
		final PomWriterOptions options = new PomWriterOptions();
		try {
			PomModuleDescriptorWriter.write(descriptor, file, options);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}



}
