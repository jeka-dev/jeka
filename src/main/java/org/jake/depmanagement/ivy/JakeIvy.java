package org.jake.depmanagement.ivy;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.XmlSettingsParser;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopedDependency;
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

	public List<JakeArtifact> resolve(JakeDependencies deps, JakeScope scope) {
		return resolve(ANONYMOUS_MODULE, deps, scope);
	}


	public List<JakeArtifact> resolve(JakeVersionedModule module, JakeDependencies deps, JakeScope scope) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId
				.newInstance(module.moduleId().group(), module.moduleId().name(), module.version().name());
		final DefaultModuleDescriptor moduleDescriptor = DefaultModuleDescriptor
				.newDefaultInstance(thisModuleRevisionId);
		for (final JakeScopedDependency scopedDependency : deps) {
			final DependencyDescriptor dependencyDescriptor = Translations
					.to(scopedDependency);
			moduleDescriptor.addDependency(dependencyDescriptor);
		}

		final String[] confs = { scope.name() };

		final ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);
		resolveOptions.setTransitive(true);
		final ResolveReport report;
		try {
			report = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (final Exception e1) {
			throw new RuntimeException(e1);
		}
		final List<JakeArtifact> result = new LinkedList<JakeArtifact>();
		for (final ArtifactDownloadReport artifactDownloadReport : report.getAllArtifactsReports()) {
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
