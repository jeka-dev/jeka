package org.jake.depmanagement.ivy;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeExternalModule;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeScopedDependency;
import org.jake.depmanagement.JakeScopedDependency.ScopeType;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionRange;
import org.jake.depmanagement.JakeVersionedModule;

final class Translations {

	/**
	 * Stands for the default configuration for publishing in ivy.
	 */
	static final JakeScope DEFAULT_CONFIGURATION = JakeScope.of("default");

	private Translations() {}

	public static DefaultModuleDescriptor to(JakeVersionedModule module, JakeDependencies dependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId
				.newInstance(module.moduleId().group(), module.moduleId().name(), module.version().name());
		final DefaultModuleDescriptor moduleDescriptor = DefaultModuleDescriptor
				.newDefaultInstance(thisModuleRevisionId);

		// Add configuration definitions
		for (final JakeScope involvedScope : dependencies.involvedScopes()) {
			final Configuration configuration = to(involvedScope);
			moduleDescriptor.addConfiguration(configuration);
		}
		if (defaultScope != null) {
			moduleDescriptor.addConfiguration(to(defaultScope));
		} else if (defaultMapping != null) {
			for (final JakeScope jakeScope : defaultMapping.involvedScopes()) {
				moduleDescriptor.addConfiguration(to(jakeScope));
			}

		}

		// Add dependency
		for (final JakeScopedDependency scopedDependency : dependencies) {
			final DependencyDescriptor dependencyDescriptor = to(scopedDependency, defaultScope, defaultMapping);
			moduleDescriptor.addDependency(dependencyDescriptor);
		}
		return moduleDescriptor;

	}

	/**
	 * @param scopedDependency must be of {@link JakeExternalModule}
	 */
	private static DependencyDescriptor to(JakeScopedDependency scopedDependency, JakeScope defaultScope, JakeScopeMapping defaultMapping) {
		final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
		final DefaultDependencyDescriptor result =  new DefaultDependencyDescriptor(to(externalModule), false);

		// filling configuration
		if (scopedDependency.scopeType() == ScopeType.SIMPLE) {
			for (final JakeScope scope : scopedDependency.scopes()) {
				result.addDependencyConfiguration(DEFAULT_CONFIGURATION.name(), scope.name());
			}
		} else if (scopedDependency.scopeType() == ScopeType.MAPPED) {
			for (final JakeScope scope : scopedDependency.scopeMapping().entries()) {
				for (final JakeScope mappedScope : scopedDependency.scopeMapping().mappedScopes(scope)) {
					result.addDependencyConfiguration(scope.name(), mappedScope.name());
				}
			}
		} else {
			if (defaultMapping != null) {
				for (final JakeScope entryScope : defaultMapping.entries()) {
					for (final JakeScope mappedScope : defaultMapping.mappedScopes(entryScope)) {
						result.addDependencyConfiguration(entryScope.name(), mappedScope.name());
					}
				}
			} else if (defaultScope != null) {
				result.addDependencyConfiguration(DEFAULT_CONFIGURATION.name(), defaultScope.name());
			}
		}
		return result;
	}

	public static Configuration to(JakeScope jakeScope) {
		final List<String> extendedScopes = new LinkedList<String>();
		for (final JakeScope parent : jakeScope.extendedScopes()) {
			extendedScopes.add(parent.name());
		}
		return new Configuration(jakeScope.name(), Visibility.PUBLIC, "", extendedScopes.toArray(new String[0]), true, null);

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

	public static JakeArtifact to(Artifact artifact, File localFile) {
		final JakeModuleId moduleId = JakeModuleId.of(artifact.getModuleRevisionId().getOrganisation(),
				artifact.getModuleRevisionId().getName());
		final JakeVersionedModule module = JakeVersionedModule.of(moduleId,
				JakeVersion.of(artifact.getModuleRevisionId().getRevision()));
		return JakeArtifact.of(module, localFile);
	}

}
