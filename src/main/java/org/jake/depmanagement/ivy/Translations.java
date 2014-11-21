package org.jake.depmanagement.ivy;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.jake.depmanagement.JakeDependency;
import org.jake.depmanagement.JakeExternalModule;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopedDependency;
import org.jake.depmanagement.JakeScopedDependency.ScopeType;
import org.jake.depmanagement.JakeVersionRange;

final class Translations {

	private Translations() {}

	public static boolean isManagedDependency(JakeDependency dependency) {
		return dependency instanceof JakeExternalModule;
	}

	/**
	 * @param scopedDependency must be of {@link JakeExternalModule}
	 */
	public static DependencyDescriptor to(JakeScopedDependency scopedDependency) {
		final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
		final DefaultDependencyDescriptor result =  new DefaultDependencyDescriptor(to(externalModule), false);

		// filling configuration
		if (scopedDependency.scopeType() == ScopeType.SIMPLE) {
			for (final JakeScope scope : scopedDependency.scopes()) {
				for (final JakeScope mappedScope : scopedDependency.mappedScopes(scope)) {
					result.addDependencyConfiguration(scope.name(), mappedScope.name());
				}
			}
		} else {
			for (final JakeScope scope : scopedDependency.scopeMapping().entries()) {
				for (final JakeScope mappedScope : scopedDependency.scopeMapping().targetScopes(scope)) {
					result.addDependencyConfiguration(scope.name(), mappedScope.name());
				}
			}
		}

		return result;
	}

	private static ModuleRevisionId to(JakeExternalModule externalModule) {
		return new ModuleRevisionId(to(externalModule.moduleId()), to(externalModule.versionRange()));
	}

	private static ModuleId to(JakeModuleId moduleId) {
		return new ModuleId(moduleId.group(), moduleId.name());
	}

	private static String to(JakeVersionRange versionRange) {
		return versionRange.definition();
	}

	private static DependencyResolver to(JakeRepo repo) {
		if (repo instanceof JakeRepo.MavenRepository) {
			final IBiblioResolver result = new IBiblioResolver();
			result.setM2compatible(true);
			result.setUseMavenMetadata(true);
			result.setRoot(repo.url().toString());
			result.setUsepoms(true);
			return result;
		}
		throw new IllegalStateException(repo.getClass().getName() + " not handled by translator.");
	}

	public static void populateIvySettingsWithRepo(IvySettings ivySettings, JakeRepos repos) {
		final boolean ivyHasYetDefaultResolver = ivySettings.getDefaultResolver() != null;
		boolean first = true;
		for(final JakeRepo jakeRepo : repos) {
			final DependencyResolver resolver = to(jakeRepo);
			resolver.setName(jakeRepo.toString());
			ivySettings.addResolver(resolver);
			if (first && !ivyHasYetDefaultResolver) {
				ivySettings.setDefaultResolver(resolver.getName());
			}
			first = false;
		}
	}




}
