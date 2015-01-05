package org.jake.depmanagement.ivy;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeExternalModule;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepo.IvyRepository;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeScopedDependency;
import org.jake.depmanagement.JakeScopedDependency.ScopeType;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionRange;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.publishing.JakeIvyPublication;
import org.jake.utils.JakeUtilsString;

final class Translations {

	/**
	 * Stands for the default configuration for publishing in ivy.
	 */
	static final JakeScope DEFAULT_CONFIGURATION = JakeScope.of("default");

	private static final String MAVEN_ARTIFACT_PATTERN = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

	private Translations() {}

	public static DefaultModuleDescriptor toUnpublished(JakeVersionedModule module, JakeDependencies dependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId
				.newInstance(module.moduleId().group(), module.moduleId().name(), module.version().name());
		final DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(thisModuleRevisionId, "integration", null);

		populateModuleDescriptor(moduleDescriptor, dependencies, defaultScope, defaultMapping);
		return moduleDescriptor;
	}

	public static DefaultModuleDescriptor toUnpublished(JakeVersionedModule module) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId
				.newInstance(module.moduleId().group(), module.moduleId().name(), module.version().name());
		return new DefaultModuleDescriptor(thisModuleRevisionId, "integration", null);
	}

	/**
	 * @param scopedDependency must be of {@link JakeExternalModule}
	 */
	private static DependencyDescriptor to(JakeScopedDependency scopedDependency, JakeScope defaultScope, JakeScopeMapping defaultMapping) {
		final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
		final ModuleRevisionId moduleRevisionId = to(externalModule);
		final boolean changing = externalModule.versionRange().definition().endsWith("-SNAPSHOT");
		final DefaultDependencyDescriptor result =
				new DefaultDependencyDescriptor(null, moduleRevisionId, false, changing, externalModule.transitive());

		// filling configuration
		if (scopedDependency.scopeType() == ScopeType.SIMPLE) {
			for (final JakeScope scope : scopedDependency.scopes()) {
				final JakeScopeMapping mapping = resolveSimple(scope, defaultScope, defaultMapping);
				for (final JakeScope fromScope : mapping.entries()) {
					for (final JakeScope mappedScope : mapping.mappedScopes(fromScope)) {
						result.addDependencyConfiguration(fromScope.name(), mappedScope.name());
					}
				}

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
		final Visibility visibility = jakeScope.isPublic() ? Visibility.PUBLIC : Visibility.PRIVATE;
		return new Configuration(jakeScope.name(), visibility, jakeScope.description(), extendedScopes.toArray(new String[0]), jakeScope.transitive(), null);
	}

	public static String[] toConfNames(JakeScope...jakeScopes) {
		final String[] result = new String[jakeScopes.length];
		for(int i=0; i < jakeScopes.length; i++) {
			result[i] = jakeScopes[i].name();
		}
		return result;
	}

	private static ModuleRevisionId to(JakeExternalModule externalModule) {
		return new ModuleRevisionId(to(externalModule.moduleId()), to(externalModule.versionRange()));
	}

	private static ModuleId to(JakeModuleId moduleId) {
		return new ModuleId(moduleId.group(), moduleId.name());
	}


	public static ModuleRevisionId to(JakeVersionedModule jakeVersionedModule) {
		return new ModuleRevisionId(to(jakeVersionedModule.moduleId()), jakeVersionedModule.version().name());
	}

	private static String to(JakeVersionRange versionRange) {
		return versionRange.definition();
	}

	// see http://www.draconianoverlord.com/2010/07/18/publishing-to-maven-repos-with-ivy.html
	private static DependencyResolver to(JakeRepo repo) {
		if (repo instanceof JakeRepo.MavenRepository) {
			if (!isFileSystem(repo.url())) {
				final IBiblioResolver result = new IBiblioResolver();
				result.setM2compatible(true);
				result.setUseMavenMetadata(true);
				result.setRoot(repo.url().toString());
				result.setUsepoms(true);
				return result;
			}
			final FileRepository fileRepo = new FileRepository(new File(repo.url().getPath()));
			final FileSystemResolver result = new FileSystemResolver();
			result.setRepository(fileRepo);
			result.addArtifactPattern(MAVEN_ARTIFACT_PATTERN);
			result.setM2compatible(true);
			return result;
		}
		final IvyRepository jakeIvyRepo = (IvyRepository) repo;
		if (isFileSystem(repo.url())) {
			final FileRepository fileRepo = new FileRepository(new File(repo.url().getPath()));
			final FileSystemResolver result = new FileSystemResolver();
			result.setRepository(fileRepo);
			for (final String pattern :  jakeIvyRepo.artifactPatterns()) {
				result.addArtifactPattern(completePattern(repo.url().getPath(), pattern));
			}
			for (final String pattern :  jakeIvyRepo.ivyPatterns()) {
				result.addIvyPattern(completePattern(repo.url().getPath(), pattern));
			}
			return result;
		}
		throw new IllegalStateException(repo + " not handled by translator.");
	}

	private static boolean isFileSystem(URL url) {
		return url.getProtocol().equals("file");
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

	private static String toIvyExpression(JakeScopeMapping scopeMapping) {
		final List<String> list = new LinkedList<String>();
		for (final JakeScope scope : scopeMapping.entries()) {
			final List<String> targets = new LinkedList<String>();
			for (final JakeScope target : scopeMapping.mappedScopes(scope)) {
				targets.add(target.name());
			}
			final String item = scope.name() + " -> " + JakeUtilsString.join(targets, ",");
			list.add(item);
		}
		return JakeUtilsString.join(list, "; ");
	}

	private static void populateModuleDescriptor(DefaultModuleDescriptor moduleDescriptor, JakeDependencies dependencies, JakeScope defaultScope, JakeScopeMapping defaultMapping) {


		// Add configuration definitions
		for (final JakeScope involvedScope : dependencies.moduleScopes()) {
			final Configuration configuration = to(involvedScope);
			moduleDescriptor.addConfiguration(configuration);
		}
		if (defaultScope != null) {
			moduleDescriptor.setDefaultConf(defaultScope.name());
		}
		if (defaultMapping != null) {
			for (final JakeScope scope : defaultMapping.involvedScopes()) {
				final Configuration configuration = to(scope);
				moduleDescriptor.addConfiguration(configuration);
			}
			moduleDescriptor.setDefaultConfMapping(toIvyExpression(defaultMapping));
		}

		// Add dependencies
		for (final JakeScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.dependency() instanceof JakeExternalModule) {
				final DependencyDescriptor dependencyDescriptor = to(scopedDependency, defaultScope, defaultMapping);
				moduleDescriptor.addDependency(dependencyDescriptor);
			}
		}
	}

	private static JakeScopeMapping resolveSimple(JakeScope scope, JakeScope defaultScope, JakeScopeMapping defaultMapping) {
		final JakeScopeMapping result;
		if (scope == null) {
			if (defaultScope == null) {
				if (defaultMapping == null) {
					result = JakeScopeMapping.of(JakeScope.of("default")).to("default");
				} else {
					result = defaultMapping;
				}
			} else {
				if (defaultMapping == null) {
					result = JakeScopeMapping.of(defaultScope).to(defaultScope);
				} else {
					result = JakeScopeMapping.of(defaultScope).to(defaultMapping.mappedScopes(defaultScope));
				}

			}
		} else {
			if (defaultMapping == null) {
				result = JakeScopeMapping.of(scope).to(scope);
			} else {
				if (defaultMapping.entries().contains(scope)) {
					result = JakeScopeMapping.of(scope).to(defaultMapping.mappedScopes(scope));
				} else {
					result = scope.mapTo(scope);
				}

			}
		}
		return result;

	}

	private static String completePattern(String url, String pattern) {
		return url + pattern;
	}

	public static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
			JakeIvyPublication publication, Date publishDate) {
		for (final JakeIvyPublication.Artifact artifact : publication) {
			for (final JakeScope jakeScope : artifact.jakeScopes) {
				if (!Arrays.asList(descriptor.getConfigurations()).contains(jakeScope.name())) {
					descriptor.addConfiguration(new Configuration(jakeScope.name()));
				}
			}
			final Artifact ivyArtifact = toPublishedArtifact(artifact, descriptor.getModuleRevisionId(), publishDate);
			for(final JakeScope jakeScope : artifact.jakeScopes) {
				descriptor.addArtifact(jakeScope.name(), ivyArtifact);
			}
		}
	}

	public static Artifact toPublishedArtifact(JakeIvyPublication.Artifact artifact, ModuleRevisionId moduleId, Date date) {
		String artifactName = artifact.file.getName();
		String extension = "";
		if (artifactName.contains(".")) {
			extension = JakeUtilsString.substringAfterFirst(artifactName, ".");
			artifactName = JakeUtilsString.substringBeforeFirst(artifactName, ".");

		}
		final String type = artifact.type != null ? artifact.type : extension;
		return new DefaultArtifact(moduleId, date, artifactName, type, extension);
	}


}
