package org.jake.depmanagement.ivy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.filter.Filter;
import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionedModule;

public final class JakeIvy {

	private static final JakeVersionedModule ANONYMOUS_MODULE = JakeVersionedModule.of(
			JakeModuleId.of("anonymousGroup", "anonymousName"), JakeVersion.of("anonymousVersion"));

	private final Ivy ivy;

	private final JakeResolutionParameters parameters;

	private JakeIvy(Ivy ivy, JakeResolutionParameters parameters) {
		super();
		this.ivy = ivy;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
		this.parameters = parameters;
	}

	public static JakeIvy of(IvySettings ivySettings, JakeResolutionParameters parameters) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new JakeIvy(ivy, parameters);
	}

	public static JakeIvy of(JakeRepos repos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithRepo(ivySettings, repos);
		return of(ivySettings, JakeResolutionParameters.of());
	}

	public static JakeIvy of() {
		return of(JakeRepos.mavenCentral());
	}

	public JakeIvy with(JakeResolutionParameters jakeResolutionParameters) {
		return new JakeIvy(this.ivy, jakeResolutionParameters);
	}

	public Set<JakeArtifact> resolve(JakeDependencies deps, JakeScope resolvedScope) {
		return resolve(ANONYMOUS_MODULE, deps, resolvedScope);
	}

	public Set<JakeArtifact> resolve(JakeVersionedModule module, JakeDependencies deps, JakeScope resolvedScope) {
		final DefaultModuleDescriptor moduleDescriptor = Translations.toUnpublished(module, deps, parameters.defaultScope(), parameters.defaultMapping());

		final ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] {resolvedScope.name()});
		resolveOptions.setTransitive(true);
		resolveOptions.setOutputReport(JakeOptions.isVerbose());
		resolveOptions.setLog(logLevel());
		resolveOptions.setRefresh(parameters.refreshed());
		resolveOptions.setArtifactFilter(new ArtifactFilter());
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
			final ModuleRevisionId moduleRevisionId = Translations.to(module);
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
		.setRefresh(false)
		.setArtifactFilter(new ArtifactFilter());
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

	public void publish() {
		ivy.p
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


	private static class ArtifactFilter implements Filter {

		@Override
		public boolean accept(Object o) {
			//	System.out.println("+++++++++++++++++++++++++++++++++++ artefact filter accept : " + o);
			return true;
		}
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

}
