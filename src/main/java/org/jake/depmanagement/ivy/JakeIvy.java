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
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopedDependency;

public final class JakeIvy {

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

	public List<File> retrieve(JakeDependencies deps, JakeScope scope) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId
				.newInstance("mygroupId", "amyrtifactId-envelope", "myversion");
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
		final List<File> result = new LinkedList<File>();
		for (final ArtifactDownloadReport artifactDownloadReport : report.getAllArtifactsReports()) {
			result.add(artifactDownloadReport.getLocalFile());

		}
		return result;
	}



}
