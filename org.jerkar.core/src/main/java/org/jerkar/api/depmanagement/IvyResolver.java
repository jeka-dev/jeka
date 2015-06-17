package org.jerkar.api.depmanagement;

import java.util.HashSet;
import java.util.List;
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
import org.apache.ivy.plugins.resolver.AbstractPatternsBasedResolver;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.jerkar.api.system.JkLog;

/**
 * Jerkar users : This class is not part of the public API !!! Please, Use {@link JkPublisher} instead.
 * Ivy wrapper providing high level methods. The API is expressed using Jerkar classes only (mostly free of Ivy classes).
 * 
 * @author Jerome Angibaud
 */
public final class IvyResolver implements InternalDepResolver {

	private static final JkVersionedModule ANONYMOUS_MODULE = JkVersionedModule.of(
			JkModuleId.of("anonymousGroup", "anonymousName"), JkVersion.ofName("anonymousVersion"));

	private final Ivy ivy;

	private IvyResolver(Ivy ivy) {
		super();
		this.ivy = ivy;
		ivy.getLoggerEngine().setDefaultLogger(new MessageLogger());
	}

	private static InternalDepResolver of(IvySettings ivySettings) {
		final Ivy ivy = Ivy.newInstance(ivySettings);
		return new IvyResolver(ivy);
	}

	/**
	 * Creates an <code>IvySettings</code> from the specified repositories.
	 */
	private static IvySettings ivySettingsOf(JkRepos resolveRepos) {
		final IvySettings ivySettings = new IvySettings();
		Translations.populateIvySettingsWithRepo(ivySettings, resolveRepos);
		return ivySettings;
	}

	/**
	 * Creates an instance using specified repository for publishing and
	 * the specified repositories for resolving.
	 */
	public static InternalDepResolver of(JkRepos resolveRepos) {
		return of(ivySettingsOf(resolveRepos));
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


	@Override
	public boolean hasMavenPublishRepo() {
		for (final DependencyResolver dependencyResolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			if (isMaven(dependencyResolver)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasIvyPublishRepo() {
		for (final DependencyResolver dependencyResolver : Translations.publishResolverOf(this.ivy.getSettings())) {
			if (!isMaven(dependencyResolver)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<JkArtifact> resolveAnonymous(JkDependencies deps, JkScope resolvedScope, JkResolutionParameters parameters) {
		return resolve(ANONYMOUS_MODULE, deps, resolvedScope, parameters);
	}

	@Override
	public Set<JkArtifact> resolve(JkVersionedModule module, JkDependencies deps, JkScope resolvedScope, JkResolutionParameters parameters) {
		final DefaultModuleDescriptor moduleDescriptor = Translations.toPublicationFreeModule(module, deps, parameters.defaultScope(), parameters.defaultMapping());

		final ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] {resolvedScope.name()});
		resolveOptions.setTransitive(true);
		resolveOptions.setOutputReport(JkLog.verbose());
		resolveOptions.setLog(logLevel());
		resolveOptions.setRefresh(parameters.refreshed());
		final ResolveReport report;
		try {
			report = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (final Exception e1) {
			throw new RuntimeException(e1);
		}
		if (report.hasError()) {
			throw new IllegalStateException("Erros while resolving dependencies : " + report.getAllProblemMessages());
		}
		final Set<JkArtifact> result = new HashSet<JkArtifact>();
		final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
		for (final String conf : resolveOptions.getConfs()) {
			result.addAll(getArtifacts(conf, artifactDownloadReports));
		}
		return result;
	}

	@Override
	public JkAttachedArtifacts getArtifacts(Iterable<JkVersionedModule> modules, JkScope ...scopes) {
		//final String defaultConf = "default";
		final DefaultModuleDescriptor moduleDescriptor = Translations.toUnpublished(ANONYMOUS_MODULE);
		for (final JkScope jkScope : scopes) {
			moduleDescriptor.addConfiguration(new Configuration(jkScope.name()));
		}
		for (final JkVersionedModule module : modules) {
			final ModuleRevisionId moduleRevisionId = Translations.toModuleRevisionId(module);
			final DefaultDependencyDescriptor dependency = new DefaultDependencyDescriptor(moduleRevisionId, true, false);
			for (final JkScope scope : scopes) {
				dependency.addDependencyConfiguration(scope.name(), scope.name());
			}
			moduleDescriptor.addDependency(dependency);
		}
		final JkAttachedArtifacts result = new JkAttachedArtifacts();
		final ResolveOptions resolveOptions = new ResolveOptions()
		.setTransitive(false)
		.setOutputReport(JkLog.verbose())
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



	private static String logLevel() {
		if (JkLog.silent()) {
			return "quiet";
		}
		if (JkLog.verbose()) {
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


}
