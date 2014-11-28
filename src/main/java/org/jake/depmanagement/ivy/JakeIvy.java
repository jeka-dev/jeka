package org.jake.depmanagement.ivy;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.XmlSettingsParser;
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

	private JakeIvy(Ivy ivy) {
		super();
		this.ivy = ivy;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
	}

	public static JakeIvy of(IvySettings ivySettings) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new JakeIvy(ivy);
	}


	public static JakeIvy of(JakeRepos repos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithRepo(ivySettings, repos);
		return of(ivySettings);
	}

	public static JakeIvy of() {
		return of(JakeRepos.mavenCentral());
	}

	public Set<JakeArtifact> resolve(JakeDependencies deps, JakeScope ... resolutionScopes) {
		return resolve(ANONYMOUS_MODULE, deps, JakeResolutionParameters.resolvedScopes(resolutionScopes));
	}

	public Set<JakeArtifact> resolve(JakeDependencies deps, JakeResolutionParameters resolutionScope) {
		return resolve(ANONYMOUS_MODULE, deps, resolutionScope);
	}

	public Set<JakeArtifact> resolve(JakeVersionedModule module, JakeDependencies deps, JakeResolutionParameters resolutionParams) {
		final DefaultModuleDescriptor moduleDescriptor = Translations.toUnpublished(module, deps, resolutionParams.defaultScope(), resolutionParams.defaultMapping());

		final ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(toConfigNames(resolutionParams.resolvedScopes()));
		resolveOptions.setTransitive(true);
		resolveOptions.setOutputReport(JakeOptions.isVerbose());
		resolveOptions.setLog(logLevel());
		resolveOptions.setRefresh(resolutionParams.refreshed());
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

	private static void parse(IvySettings ivySettings, File jakeHome, File projectDir) {
		final File globalSettingsXml = new File(jakeHome, "ivy/ivysettings.xml");
		if (globalSettingsXml.exists()) {
			try {
				new XmlSettingsParser(ivySettings).parse(globalSettingsXml.toURI().toURL());
			} catch (final Exception e) {
				throw new IllegalStateException("Can't parse Ivy settings file", e);
			}
		}
		final File settingsXml = new File(projectDir, "build/ivysettings.xml");
		if (settingsXml.exists()) {
			try {
				new XmlSettingsParser(ivySettings).parse(settingsXml.toURI().toURL());
			} catch (final Exception e) {
				throw new IllegalStateException("Can't parse Ivy settings file", e);
			}
		}
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

	private static String[] toConfigNames(Iterable<JakeScope> scopes) {
		final List<String> list = new LinkedList<String>();
		for (final JakeScope scope : scopes) {
			list.add(scope.name());
		}
		return list.toArray(new String[list.size()]);
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
			System.out.println("+++++++++++++++++++++++++++++++++++" + o);
			return true;
		}

	}



}
