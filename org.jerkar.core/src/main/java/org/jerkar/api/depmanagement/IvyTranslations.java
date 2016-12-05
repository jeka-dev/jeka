package org.jerkar.api.depmanagement;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.IvyRepResolver;
import org.apache.ivy.plugins.resolver.RepositoryResolver;
import org.apache.ivy.util.url.CredentialsStore;
import org.jerkar.api.depmanagement.JkMavenPublication.JkClassifiedArtifact;
import org.jerkar.api.depmanagement.JkRepo.JkIvyRepository;
import org.jerkar.api.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;

final class IvyTranslations {

    private static final Map<String, String> ALGOS = JkUtilsIterable.mapOf("MD5", "md5", "SHA-1",
            "sha1");

    private static final String MAIN_RESOLVER_NAME = "MAIN";

    private static final String EXTRA_NAMESPACE = "http://ant.apache.org/ivy/extra";

    private static final String EXTRA_PREFIX = "e";

    private static final String PUBLISH_RESOLVER_NAME = "publisher:";

    private static final String MAVEN_ARTIFACT_PATTERN = "/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

    private static final Configuration DEFAULT_CONFIGURATION = new Configuration("default");

    private IvyTranslations() {
    }

    public static DefaultModuleDescriptor toPublicationLessModule(JkVersionedModule module,
            JkDependencies dependencies, JkScopeMapping defaultMapping,
            JkVersionProvider resolvedVersions) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .moduleId().group(), module.moduleId().name(), module.version().name());
        final DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(
                thisModuleRevisionId, "integration", null);

        populateModuleDescriptor(moduleDescriptor, dependencies, defaultMapping, resolvedVersions);
        return moduleDescriptor;
    }

    public static DefaultModuleDescriptor toUnpublished(JkVersionedModule module) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .moduleId().group(), module.moduleId().name(), module.version().name());
        return new DefaultModuleDescriptor(thisModuleRevisionId, "integration", null);
    }

    private static DependencyDescriptor toScopelessDependencyDescriptor(
            JkModuleDependency dependency, JkScopeMapping defaultMapping,
            JkVersion resolvedVersion, Configuration[] moduleConfigurations) {

        final ModuleRevisionId moduleRevisionId = toModuleRevisionId(dependency, resolvedVersion);
        final boolean changing = dependency.versionRange().definition().endsWith("-SNAPSHOT");
        final DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null,
                moduleRevisionId, false, changing, dependency.transitive());

        // filling configuration
        if (defaultMapping == null || defaultMapping.entries().isEmpty()) {
            result.addDependencyConfiguration("*", "*");
        } else {
            for (final JkScope entryScope : defaultMapping.entries()) {
                for (final JkScope mappedScope : defaultMapping.mappedScopes(entryScope)) {
                    result.addDependencyConfiguration(entryScope.name(), mappedScope.name());
                }
            }
        }

        for (final JkDepExclude depExclude : dependency.excludes()) {
            final ExcludeRule excludeRule = toExcludeRule(depExclude);
            result.addExcludeRule("*", excludeRule);
        }
        return result;
    }

    /**
     * @param scopedDependency
     *            must be of {@link JkModuleDependency}
     */
    private static DependencyDescriptor toDependencyDescriptor(JkScopedDependency scopedDependency,
            JkScopeMapping defaultMapping, JkVersion resolvedVersion) {
        final JkModuleDependency moduleDep = (JkModuleDependency) scopedDependency.dependency();
        final ModuleRevisionId moduleRevisionId = toModuleRevisionId(moduleDep, resolvedVersion);
        final boolean changing = moduleDep.versionRange().definition().endsWith("-SNAPSHOT");
        final DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null,
                moduleRevisionId, false, changing, moduleDep.transitive());

        // filling configuration
        if (scopedDependency.scopeType() == ScopeType.SIMPLE) {
            for (final JkScope scope : scopedDependency.scopes()) {
                final JkScopeMapping mapping = resolveSimple(scope, defaultMapping);
                for (final JkScope fromScope : mapping.entries()) {
                    for (final JkScope mappedScope : mapping.mappedScopes(fromScope)) {
                        result.addDependencyConfiguration(fromScope.name(), mappedScope.name());
                    }
                }

            }
        } else if (scopedDependency.scopeType() == ScopeType.MAPPED) {
            for (final JkScope scope : scopedDependency.scopeMapping().entries()) {
                for (final JkScope mappedScope : scopedDependency.scopeMapping()
                        .mappedScopes(scope)) {
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
            }
        }
        for (final JkDepExclude depExclude : moduleDep.excludes()) {
            final ExcludeRule excludeRule = toExcludeRule(depExclude);
            result.addExcludeRule("*", excludeRule);
        }
        return result;
    }

    private static DefaultExcludeRule toExcludeRule(JkDepExclude depExclude) {
        final String type = depExclude.type() == null ? PatternMatcher.ANY_EXPRESSION : depExclude
                .type();
        final String ext = depExclude.ext() == null ? PatternMatcher.ANY_EXPRESSION : depExclude
                .ext();
        final ArtifactId artifactId = new ArtifactId(toModuleId(depExclude.moduleId()), "*", type,
                ext);
        final DefaultExcludeRule result = new DefaultExcludeRule(artifactId,
                ExactPatternMatcher.INSTANCE, null);
        for (final JkScope scope : depExclude.getScopes()) {
            result.addConfiguration(scope.name());
        }
        return result;
    }

    private static Configuration toConfiguration(JkScope jkScope) {
        final List<String> extendedScopes = new LinkedList<String>();
        for (final JkScope parent : jkScope.extendedScopes()) {
            extendedScopes.add(parent.name());
        }
        final Visibility visibility = Visibility.PUBLIC;
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

    public static ModuleRevisionId toModuleRevisionId(JkModuleDependency moduleDependency,
            JkVersion resolvedVersion) {
        final String originalVersion = moduleDependency.versionRange().definition();
        if (resolvedVersion == null || resolvedVersion.name().equals(originalVersion)) {
            return new ModuleRevisionId(toModuleId(moduleDependency.moduleId()), originalVersion);
        }
        final Map<String, String> extra = JkUtilsIterable.mapOf("revConstraints", originalVersion);
        if (moduleDependency.ext() != null) {
            extra.put("ext", moduleDependency.ext());
        }
        return ModuleRevisionId.newInstance(moduleDependency.moduleId().group(), moduleDependency
                .moduleId().name(), resolvedVersion.name(), extra);

    }

    private static ModuleId toModuleId(JkModuleId moduleId) {
        return new ModuleId(moduleId.group(), moduleId.name());
    }

    public static ModuleRevisionId toModuleRevisionId(JkVersionedModule jkVersionedModule) {
        return new ModuleRevisionId(toModuleId(jkVersionedModule.moduleId()), jkVersionedModule
                .version().name());
    }

    public static JkVersionedModule toJkVersionedModule(ModuleRevisionId moduleRevisionId) {
        return JkVersionedModule.of(
                JkModuleId.of(moduleRevisionId.getOrganisation(), moduleRevisionId.getName()),
                JkVersion.ofName(moduleRevisionId.getRevision()));
    }

    // see
    // http://www.draconianoverlord.com/2010/07/18/publishing-to-maven-repos-with-ivy.html
    private static DependencyResolver toResolver(JkRepo repo, Set<String> digesterAlgorithms) {
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
                }
                ;
                result.setChangingPattern("\\*-SNAPSHOT");
                result.setCheckmodified(true);
                if (!digesterAlgorithms.isEmpty()) {
                    result.setChecksums(JkUtilsString.join(ivyNameAlgos(digesterAlgorithms), ","));
                }
                return result;
            }
            final FileRepository fileRepo = new FileRepository(new File(repo.url().getPath()));
            final FileSystemResolver result = new FileSystemResolver();
            result.setRepository(fileRepo);
            result.addArtifactPattern(completePattern(repo.url().getPath(), MAVEN_ARTIFACT_PATTERN));
            result.setM2compatible(true);
            if (!digesterAlgorithms.isEmpty()) {
                result.setChecksums(JkUtilsString.join(ivyNameAlgos(digesterAlgorithms), ","));
            }
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
            if (!digesterAlgorithms.isEmpty()) {
                result.setChecksums(JkUtilsString.join(ivyNameAlgos(digesterAlgorithms), ","));
            }
            return result;
        }
        if (repo.url().getProtocol().equals("http")) {
            final IvyRepResolver result = new IvyRepResolver();
            result.setIvyroot(repo.url().toString());
            result.setArtroot(repo.url().toString());
            result.setArtpattern(IvyRepResolver.DEFAULT_IVYPATTERN);
            result.setIvypattern(IvyRepResolver.DEFAULT_IVYPATTERN);
            result.setM2compatible(false);
            if (isHttp(repo.url())) {
                if (!CredentialsStore.INSTANCE.hasCredentials(repo.url().getHost())) {
                    CredentialsStore.INSTANCE.addCredentials(repo.realm(), repo.url().getHost(),
                            repo.userName(), repo.password());

                }
            }
            ;
            result.setChangingPattern("\\*-SNAPSHOT");
            result.setCheckmodified(true);
            if (!digesterAlgorithms.isEmpty()) {
                result.setChecksums(JkUtilsString.join(ivyNameAlgos(digesterAlgorithms), ","));
            }
            return result;
        }

        throw new IllegalStateException(repo + " not handled by translator.");
    }

    private static Set<String> ivyNameAlgos(Set<String> algos) {
        final Set<String> result = new HashSet<String>();
        for (final String algo : algos) {
            result.add(ALGOS.get(algo));
        }
        return result;
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

    public static void populateIvySettingsWithPublishRepo(IvySettings ivySettings,
            JkPublishRepos repos) {
        for (final JkPublishRepo repo : repos) {
            final DependencyResolver resolver = toResolver(repo.repo(), repo.checksumAlgorithms());
            resolver.setName(PUBLISH_RESOLVER_NAME + repo.repo().url());
            ivySettings.addResolver(resolver);
        }
    }

    public static String publishResolverUrl(DependencyResolver resolver) {
        return resolver.getName().substring(PUBLISH_RESOLVER_NAME.length());
    }

    public static List<RepositoryResolver> publishResolverOf(IvySettings ivySettings) {
        final List<RepositoryResolver> resolvers = new LinkedList<RepositoryResolver>();
        for (final Object resolverObject : ivySettings.getResolvers()) {
            final RepositoryResolver resolver = (RepositoryResolver) resolverObject;
            if (resolver.getName() != null && resolver.getName().startsWith(PUBLISH_RESOLVER_NAME)) {
                resolvers.add(resolver);
            }
        }
        return resolvers;
    }

    @SuppressWarnings("unchecked")
    private static ChainResolver toChainResolver(JkRepos repos) {
        final ChainResolver chainResolver = new ChainResolver();
        for (final JkRepo jkRepo : repos) {
            final DependencyResolver resolver = toResolver(jkRepo, Collections.EMPTY_SET);
            resolver.setName(jkRepo.toString());
            chainResolver.add(resolver);
        }
        return chainResolver;
    }

    public static JkVersionedModule toJkVersionedModule(Artifact artifact) {
        final JkModuleId moduleId = JkModuleId.of(artifact.getModuleRevisionId().getOrganisation(),
                artifact.getModuleRevisionId().getName());
        return JkVersionedModule.of(moduleId,
                JkVersion.ofName(artifact.getModuleRevisionId().getRevision()));
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
            JkDependencies dependencies, JkScopeMapping defaultMapping,
            JkVersionProvider resolvedVersions) {

        // Add configuration definitions
        for (final JkScope involvedScope : dependencies.involvedScopes()) {
            final Configuration configuration = toConfiguration(involvedScope);
            moduleDescriptor.addConfiguration(configuration);
        }
        if (dependencies.involvedScopes().isEmpty()) {
            moduleDescriptor.addConfiguration(DEFAULT_CONFIGURATION);
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
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .dependency();
                final JkVersion resolvedVersion = resolvedVersions.versionOf(externalModule
                        .moduleId());
                final DependencyDescriptor dependencyDescriptor;
                if (scopedDependency.scopeType() == ScopeType.UNSET) {
                    dependencyDescriptor = toScopelessDependencyDescriptor(externalModule,
                            defaultMapping, resolvedVersion, moduleDescriptor.getConfigurations());
                } else {
                    dependencyDescriptor = toDependencyDescriptor(scopedDependency, defaultMapping,
                            resolvedVersion);
                }

                moduleDescriptor.addDependency(dependencyDescriptor);
            }
        }

        // Add excludes
        for (final JkDepExclude exclude : dependencies.excludes()) {
            final DefaultExcludeRule rule = toExcludeRule(exclude);
            if (exclude.getScopes().isEmpty()) {
                for (final JkScope involvedScope : dependencies.involvedScopes()) {
                    rule.addConfiguration(involvedScope.name());
                }
            }
            moduleDescriptor.addExcludeRule(rule);
        }

        // Add version override for transitive dependency
        for (final JkModuleId moduleId : resolvedVersions.moduleIds()) {
            final JkVersion version = resolvedVersions.versionOf(moduleId);
            moduleDescriptor.addDependencyDescriptorMediator(toModuleId(moduleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new OverrideDependencyDescriptorMediator(null, version.name()));
        }

    }

    private static JkScopeMapping resolveSimple(JkScope scope, JkScopeMapping defaultMapping) {
        final JkScopeMapping result;
        if (scope == null) {
            if (defaultMapping == null) {
                result = JkScopeMapping.of(JkScope.of("default")).to("default");
            } else {
                result = defaultMapping;
            }
        } else {
            if (defaultMapping == null) {
                result = JkScopeMapping.of(scope).to(scope);
            } else {
                if (defaultMapping.entries().contains(scope)) {
                    result = JkScopeMapping.of(scope).to(defaultMapping.mappedScopes(scope));
                } else {
                    result = scope.mapTo(scope.name() + "(default)");
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
            for (final JkScope jkScope : JkScope.involvedScopes(artifact.jkScopes)) {
                if (!Arrays.asList(descriptor.getConfigurations()).contains(jkScope.name())) {
                    descriptor.addConfiguration(toConfiguration(jkScope));
                }
            }
            final Artifact ivyArtifact = toPublishedArtifact(artifact,
                    descriptor.getModuleRevisionId(), publishDate);
            for (final JkScope jkScope : JkScope.involvedScopes(artifact.jkScopes)) {
                descriptor.addArtifact(jkScope.name(), ivyArtifact);
            }
        }
    }

    public static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
            JkMavenPublication publication, Date publishDate) {

        final ModuleRevisionId moduleRevisionId = descriptor.getModuleRevisionId();
        final String artifactName = moduleRevisionId.getName();
        final Artifact mavenMainArtifact = toPublishedMavenArtifact(publication.mainArtifactFiles()
                .get(0), artifactName, null, moduleRevisionId, publishDate);
        final String mainConf = "default";
        populateDescriptorWithMavenArtifact(descriptor, mainConf, mavenMainArtifact);

        for (final JkClassifiedArtifact artifactEntry : publication.classifiedArtifacts()) {
            final File file = artifactEntry.file();
            final String classifier = artifactEntry.classifier();
            final Artifact mavenArtifact = toPublishedMavenArtifact(file, artifactName, classifier,
                    descriptor.getModuleRevisionId(), publishDate);
            final String conf = classifier;
            populateDescriptorWithMavenArtifact(descriptor, conf, mavenArtifact);
        }
    }

    private static void populateDescriptorWithMavenArtifact(DefaultModuleDescriptor descriptor,
            String conf, Artifact artifact) {
        if (descriptor.getConfiguration(conf) == null) {
            descriptor.addConfiguration(new Configuration(conf));
        }
        descriptor.addArtifact(conf, artifact);
        descriptor.addExtraAttributeNamespace(EXTRA_PREFIX, EXTRA_NAMESPACE);
    }

    public static Artifact toPublishedArtifact(JkIvyPublication.Artifact artifact,
            ModuleRevisionId moduleId, Date date) {
        final String artifactName = JkUtilsString.isBlank(artifact.name) ? moduleId.getName()
                : artifact.name;
        final String extension = JkUtilsObject.firstNonNull(artifact.extension, "");
        final String type = JkUtilsObject.firstNonNull(artifact.type, extension);
        return new DefaultArtifact(moduleId, date, artifactName, type, extension);
    }

    private static Artifact toPublishedMavenArtifact(File artifact, String artifactName,
            String classifier, ModuleRevisionId moduleId, Date date) {
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


}
