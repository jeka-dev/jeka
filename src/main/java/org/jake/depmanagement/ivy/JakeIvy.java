package org.jake.depmanagement.ivy;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.XmlSettingsParser;
import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionScope;
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

	public List<JakeArtifact> resolve(JakeDependencies deps, JakeScope resolutionScope) {
		return resolve(ANONYMOUS_MODULE, deps, JakeResolutionScope.of(resolutionScope));
	}

	public List<JakeArtifact> resolve(JakeDependencies deps, JakeResolutionScope resolutionScope) {
		return resolve(ANONYMOUS_MODULE, deps, resolutionScope);
	}

	public List<JakeArtifact> resolve(JakeVersionedModule module, JakeDependencies deps, JakeResolutionScope resolutionScope) {
		final DefaultModuleDescriptor moduleDescriptor = Translations.to(module, deps, resolutionScope.defaultScope(), resolutionScope.defaultMapping());

		final ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] {"*"/*resolutionScope.dependencyScope().name()*/});
		resolveOptions.setTransitive(true);
		resolveOptions.setOutputReport(JakeOptions.isVerbose());
		final ResolveReport report;
		try {
			report = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (final Exception e1) {
			throw new RuntimeException(e1);
		}
		final ConfigurationResolveReport configReport = report.getConfigurationReport("default");
		final List<JakeArtifact> result = new LinkedList<JakeArtifact>();
		System.out.println("----------------------------------------------------------------" + configReport.getArtifactsNumber());
		for (final ArtifactDownloadReport artifactDownloadReport : configReport.getAllArtifactsReports()) {
			result.add(Translations.to(artifactDownloadReport.getArtifact(),
					artifactDownloadReport.getLocalFile()));

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




}
