package org.jerkar.internal.ivy;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.util.url.CredentialsStore;
import org.jerkar.depmanagement.JkArtifact;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkExternalModule;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkRepo.JkIvyRepository;
import org.jerkar.depmanagement.JkRepos;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkScopedDependency;
import org.jerkar.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.depmanagement.JkVersionRange;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkPublishRepos;
import org.jerkar.publishing.JkPublishRepos.JkPublishRepo;
import org.jerkar.utils.JkUtilsIterable;
import org.jerkar.utils.JkUtilsString;

final class Translations {

	private static final String MAIN_RESOLVER_NAME = "MAIN";

	private static final String EXTRA_NAMESPACE = "http://ant.apache.org/ivy/extra";

	private static final String EXTRA_PREFIX = "e";

	private static final String PUBLISH_RESOLVER_NAME = "publisher:";

	/**
	 * Stands for the default configuration for publishing in ivy.
	 */
	static final JkScope DEFAULT_CONFIGURATION = JkScope.of("default");

	private static final String MAVEN_ARTIFACT_PATTERN = "/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

	private Translations() {
	}

	public static DefaultModuleDescriptor toPublicationFreeModule(JkVersionedModule module,
			JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module.moduleId().group(), module
				.moduleId().name(), module.version().name());
		final DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(thisModuleRevisionId,
				"integration", null);

		populateModuleDescriptor(moduleDescriptor, dependencies, defaultScope, defaultMapping);
		return moduleDescriptor;
	}

	public static DefaultModuleDescriptor toUnpublished(JkVersionedModule module) {
		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module.moduleId().group(), module
				.moduleId().name(), module.version().name());
		return new DefaultModuleDescriptor(thisModuleRevisionId, "integration", null);
	}

	/**
	 * @param scopedDependency
	 *            must be of {@link JkExternalModule}
	 */
	private static DependencyDescriptor to(JkScopedDependency scopedDependency, JkScope defaultScope,
			JkScopeMapping defaultMapping) {
		final JkExternalModule externalModule = (JkExternalModule) scopedDependency.dependency();
		final ModuleRevisionId moduleRevisionId = to(externalModule);
		final boolean changing = externalModule.versionRange().definition().endsWith("-SNAPSHOT");
		final DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null, moduleRevisionId, false,
				changing, externalModule.transitive());

		// filling configuration
		if (scopedDependency.scopeType() == ScopeType.SIMPLE) {
			for (final JkScope scope : scopedDependency.scopes()) {
				final JkScopeMapping mapping = resolveSimple(scope, defaultScope, defaultMapping);
				for (final JkScope fromScope : mapping.entries()) {
					for (final JkScope mappedScope : mapping.mappedScopes(fromScope)) {
						result.addDependencyConfiguration(fromScope.name(), mappedScope.name());
					}
				}

			}
		} else if (scopedDependency.scopeType() == ScopeType.MAPPED) {
			for (final JkScope scope : scopedDependency.scopeMapping().entries()) {
				for (final JkScope mappedScope : scopedDependency.scopeMapping().mappedScopes(scope)) {
					result.addDependencyConfiguration(scope.name(), mappedScope.name());
				}
			}
		} else {
			if (defaultMapping != null) {
				for (final JkScope entryScope : defaultMapping.entries()) {
					for (final JkScope mappedScope : defaultMapping.mappedScopes(entryScope)) {
						result.addDependencyConfiguration(entryScope.name(), mappedScope.name());
					}
				}
			} else if (defaultScope != null) {
				result.addDependencyConfiguration(DEFAULT_CONFIGURATION.name(), defaultScope.name());
			}
		}
		return result;
	}

	public static Configuration toConfiguration(JkScope jkScope) {
		final List<String> extendedScopes = new LinkedList<String>();
		for (final JkScope parent : jkScope.extendedScopes()) {
			extendedScopes.add(parent.name());
		}
		final Visibility visibility = jkScope.isPublic() ? Visibility.PUBLIC : Visibility.PRIVATE;
		return new Configuration(jkScope.name(), visibility, jkScope.description(),
				extendedScopes.toArray(new String[0]), jkScope.transitive(), null);
	}

	public static String[] toConfNames(JkScope... jkScopes) {
		final String[] result = new String[jkScopes.length];
		for (int i = 0; i < jkScopes.length; i++) {
			result[i] = jkScopes[i].name();
		}
		return result;
	}

	private static ModuleRevisionId to(JkExternalModule externalModule) {
		return new ModuleRevisionId(toModuleId(externalModule.moduleId()), toString(externalModule.versionRange()));
	}

	private static ModuleId toModuleId(JkModuleId moduleId) {
		return new ModuleId(moduleId.group(), moduleId.name());
	}

	public static ModuleRevisionId toModuleRevisionId(JkVersionedModule jkVersionedModule) {
		return new ModuleRevisionId(toModuleId(jkVersionedModule.moduleId()), jkVersionedModule.version().name());
	}

	public static JkVersionedModule toJerkarVersionedModule(ModuleRevisionId moduleRevisionId) {
		return JkVersionedModule.of(JkModuleId.of(moduleRevisionId.getOrganisation(), moduleRevisionId.getName()),
				JkVersion.ofName(moduleRevisionId.getRevision()));
	}

	private static String toString(JkVersionRange versionRange) {
		return versionRange.definition();
	}

	// see
	// http://www.draconianoverlord.com/2010/07/18/publishing-to-maven-repos-with-ivy.html
	public static DependencyResolver toResolver(JkRepo repo) {
		if (repo instanceof JkRepo.JkMavenRepository) {
			if (!isFileSystem(repo.url())) {
				final IBiblioResolver result = new IBiblioResolver();
				result.setM2compatible(true);
				result.setUseMavenMetadata(true);
				result.setRoot(repo.url().toString());
				result.setUsepoms(true);
				if (isHttp(repo.url())) {
					if (!CredentialsStore.INSTANCE.hasCredentials(repo.url().getHost())) {
						CredentialsStore.INSTANCE.addCredentials(repo.realm(),
								repo.url().getHost(), repo.userName(), repo.password());

					}
				};
				result.setChangingPattern("*-SNAPSHOT");
				result.setCheckmodified(true);
				return result;
			}
			final FileRepository fileRepo = new FileRepository(new File(repo.url().getPath()));
			final FileSystemResolver result = new FileSystemResolver();
			result.setRepository(fileRepo);
			result.addArtifactPattern(completePattern(repo.url().getPath(), MAVEN_ARTIFACT_PATTERN));
			result.setM2compatible(true);
			return result;
		}
		final JkIvyRepository jkIvyRepo = (JkIvyRepository) repo;
		if (isFileSystem(repo.url())) {
			final FileRepository fileRepo = new FileRepository(new File(repo.url().getPath()));
			final FileSystemResolver result = new FileSystemResolver();
			result.setRepository(fileRepo);
			for (final String pattern : jkIvyRepo.artifactPatterns()) {
				result.addArtifactPattern(completePattern(repo.url().getPath(), pattern));
			}
			for (final String pattern : jkIvyRepo.ivyPatterns()) {
				result.addIvyPattern(completePattern(repo.url().getPath(), pattern));
			}
			return result;
		}
		throw new IllegalStateException(repo + " not handled by translator.");
	}

	private static boolean isFileSystem(URL url) {
		return url.getProtocol().equals("file");
	}

	private static boolean isHttp(URL url) {
		return url.getProtocol().equals("http") || url.getProtocol().equals("https");
	}

	public static void populateIvySettingsWithRepo(IvySettings ivySettings, JkRepos repos) {
		final DependencyResolver resolver = toChainResolver(repos);
		resolver.setName(MAIN_RESOLVER_NAME);
		ivySettings.addResolver(resolver);
		ivySettings.setDefaultResolver(MAIN_RESOLVER_NAME);
	}

	public static void populateIvySettingsWithPublishRepo(IvySettings ivySettings, JkPublishRepos repos) {
		for (final JkPublishRepo repo : repos) {
			final DependencyResolver resolver = toResolver(repo.repo());
			resolver.setName(PUBLISH_RESOLVER_NAME + repo.repo().url());
			ivySettings.addResolver(resolver);
		}
	}

	public static String publishResolverUrl(DependencyResolver resolver ) {
		return resolver.getName().substring(PUBLISH_RESOLVER_NAME.length());
	}

	public static List<DependencyResolver> publishResolverOf(IvySettings ivySettings) {
		final List<DependencyResolver> resolvers = new LinkedList<DependencyResolver>();
		for (final Object resolverObject : ivySettings.getResolvers()) {
			final DependencyResolver resolver = (DependencyResolver) resolverObject;
			if (resolver.getName() != null && resolver.getName().startsWith(PUBLISH_RESOLVER_NAME)) {
				resolvers.add(resolver);
			}
		}
		return resolvers;
	}

	private static ChainResolver toChainResolver(JkRepos repos) {
		final ChainResolver chainResolver = new ChainResolver();
		for (final JkRepo jkRepo : repos) {
			final DependencyResolver resolver = toResolver(jkRepo);
			resolver.setName(jkRepo.toString());
			chainResolver.add(resolver);
		}
		return chainResolver;
	}

	public static JkArtifact to(Artifact artifact, File localFile) {
		final JkModuleId moduleId = JkModuleId.of(artifact.getModuleRevisionId().getOrganisation(), artifact
				.getModuleRevisionId().getName());
		final JkVersionedModule module = JkVersionedModule.of(moduleId,
				JkVersion.ofName(artifact.getModuleRevisionId().getRevision()));
		return JkArtifact.of(module, localFile);
	}

	private static String toIvyExpression(JkScopeMapping scopeMapping) {
		final List<String> list = new LinkedList<String>();
		for (final JkScope scope : scopeMapping.entries()) {
			final List<String> targets = new LinkedList<String>();
			for (final JkScope target : scopeMapping.mappedScopes(scope)) {
				targets.add(target.name());
			}
			final String item = scope.name() + " -> " + JkUtilsString.join(targets, ",");
			list.add(item);
		}
		return JkUtilsString.join(list, "; ");
	}

	private static void populateModuleDescriptor(DefaultModuleDescriptor moduleDescriptor,
			JkDependencies dependencies, JkScope defaultScope, JkScopeMapping defaultMapping) {

		// Add configuration definitions
		for (final JkScope involvedScope : dependencies.declaredScopes()) {
			final Configuration configuration = toConfiguration(involvedScope);
			moduleDescriptor.addConfiguration(configuration);
		}
		if (defaultScope != null) {
			moduleDescriptor.setDefaultConf(defaultScope.name());
		}
		if (defaultMapping != null) {
			for (final JkScope scope : defaultMapping.entries()) {
				final Configuration configuration = toConfiguration(scope);
				moduleDescriptor.addConfiguration(configuration);
			}
			moduleDescriptor.setDefaultConfMapping(toIvyExpression(defaultMapping));
		}

		// Add dependencies
		for (final JkScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.dependency() instanceof JkExternalModule) {
				final DependencyDescriptor dependencyDescriptor = to(scopedDependency, defaultScope, defaultMapping);
				moduleDescriptor.addDependency(dependencyDescriptor);
			}
		}

	}


	private static JkScopeMapping resolveSimple(JkScope scope, JkScope defaultScope,
			JkScopeMapping defaultMapping) {
		final JkScopeMapping result;
		if (scope == null) {
			if (defaultScope == null) {
				if (defaultMapping == null) {
					result = JkScopeMapping.of(JkScope.of("default")).to("default");
				} else {
					result = defaultMapping;
				}
			} else {
				if (defaultMapping == null) {
					result = JkScopeMapping.of(defaultScope).to(defaultScope);
				} else {
					result = JkScopeMapping.of(defaultScope).to(defaultMapping.mappedScopes(defaultScope));
				}

			}
		} else {
			if (defaultMapping == null) {
				result = JkScopeMapping.of(scope).to(scope);
			} else {
				if (defaultMapping.entries().contains(scope)) {
					result = JkScopeMapping.of(scope).to(defaultMapping.mappedScopes(scope));
				} else {
					result = scope.mapTo(scope);
				}

			}
		}
		return result;

	}

	private static String completePattern(String url, String pattern) {
		return url + "/" + pattern;
	}

	public static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
			JkIvyPublication publication, Date publishDate) {
		for (final JkIvyPublication.Artifact artifact : publication) {
			for (final JkScope jkScope : artifact.jkScopes) {
				if (!Arrays.asList(descriptor.getConfigurations()).contains(jkScope.name())) {
					descriptor.addConfiguration(toConfiguration(jkScope));
				}
			}
			final Artifact ivyArtifact = toPublishedArtifact(artifact, descriptor.getModuleRevisionId(), publishDate);
			for (final JkScope jkScope : artifact.jkScopes) {
				descriptor.addArtifact(jkScope.name(), ivyArtifact);
			}
		}
	}

	public static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
			JkMavenPublication publication, Date publishDate) {

		final Artifact mavenMainArtifact = toPublishedMavenArtifact(publication.mainArtifactFile(), publication.artifactName(),
				null ,descriptor.getModuleRevisionId(), publishDate);
		final String mainConf = "default";
		populateDescriptorWithMavenArtifact(descriptor, mainConf, mavenMainArtifact);

		for (final Map.Entry<String, File> artifactEntry : publication.extraArtifacts().entrySet()) {
			final File file = artifactEntry.getValue();
			final String classifier = artifactEntry.getKey();
			final Artifact mavenArtifact = toPublishedMavenArtifact(file, publication.artifactName(),
					classifier ,descriptor.getModuleRevisionId(), publishDate);
			final String conf = classifier;
			populateDescriptorWithMavenArtifact(descriptor, conf, mavenArtifact);
		}
	}

	private static void populateDescriptorWithMavenArtifact(DefaultModuleDescriptor descriptor, String conf, Artifact artifact) {
		if (descriptor.getConfiguration(conf) == null) {
			descriptor.addConfiguration(new Configuration(conf));
		}
		descriptor.addArtifact(conf, artifact);
		descriptor.addExtraAttributeNamespace(EXTRA_PREFIX, EXTRA_NAMESPACE);
	}



	public static Artifact toPublishedArtifact(JkIvyPublication.Artifact artifact, ModuleRevisionId moduleId,
			Date date) {
		String artifactName = artifact.file.getName();
		String extension = "";
		if (artifactName.contains(".")) {
			extension = JkUtilsString.substringAfterFirst(artifactName, ".");
			artifactName = JkUtilsString.substringBeforeFirst(artifactName, ".");

		}
		final String type = artifact.type != null ? artifact.type : extension;
		return new DefaultArtifact(moduleId, date, artifactName, type, extension);
	}

	public static Artifact toPublishedMavenArtifact(File artifact, String artifactName, String classifier, ModuleRevisionId moduleId,
			Date date) {
		final String extension = JkUtilsString.substringAfterLast(artifact.getName(), ".");
		final String type = extension;
		final Map<String, String> extraMap;
		if (classifier == null) {
			extraMap = new HashMap<String, String>();
		} else {
			extraMap = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
		}
		return new DefaultArtifact(moduleId, date, artifactName, type, extension, extraMap);
	}

	public static Map<JkModuleId, JkVersion> toModuleVersionMap(Properties props) {
		final Map<JkModuleId, JkVersion> result = new HashMap<JkModuleId, JkVersion>();
		for (final Object depMridObject : props.keySet()) {
			final String depMridStr = (String) depMridObject;
			final String[] parts = props.getProperty(depMridStr).split(" ");
			final ModuleRevisionId decodedMrid = ModuleRevisionId.decode(depMridStr);
			final JkModuleId jkModuleId = JkModuleId.of(decodedMrid.getOrganisation(), decodedMrid.getName());
			final JkVersion resolvedOrForcedVersion = JkVersion.ofName(parts[2]);
			result.put(jkModuleId, resolvedOrForcedVersion);
		}
		return result;
	}
}
