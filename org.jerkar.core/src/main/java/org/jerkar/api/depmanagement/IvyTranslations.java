package org.jerkar.api.depmanagement;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
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
import org.jerkar.api.utils.JkUtilsReflect;
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

    private static final String DEFAULT_EXTENSION = "jar";

    private IvyTranslations() {
    }

    static DefaultModuleDescriptor toPublicationLessModule(JkVersionedModule module,
            JkDependencies dependencies, JkScopeMapping defaultMapping,
            JkVersionProvider resolvedVersions, IvySettings ivySettings) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .moduleId().group(), module.moduleId().name(), module.version().name());
        final DefaultModuleDescriptor result = new DefaultModuleDescriptor(
                thisModuleRevisionId, "integration", null);

        populateModuleDescriptor(result, dependencies, defaultMapping, resolvedVersions, ivySettings);
        return result;
    }



    private static DefaultExcludeRule toExcludeRule(JkDepExclude depExclude, Iterable<String> allRootConfs) {
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
        if (depExclude.getScopes().isEmpty()) {
            for (final String conf : allRootConfs) {
                result.addConfiguration(conf);
            }

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


    static ModuleRevisionId toModuleRevisionId(JkModuleId moduleId, JkVersionRange versionRange) {
        final String originalVersion = versionRange.definition();
        final Map<String, String> extra = new HashMap<String, String>();
        return ModuleRevisionId.newInstance(moduleId.group(), moduleId.name(), originalVersion, extra);
    }

    private static ModuleId toModuleId(JkModuleId moduleId) {
        return new ModuleId(moduleId.group(), moduleId.name());
    }

    static ModuleRevisionId toModuleRevisionId(JkVersionedModule jkVersionedModule) {
        return new ModuleRevisionId(toModuleId(jkVersionedModule.moduleId()), jkVersionedModule
                .version().name());
    }

    static JkVersionedModule toJkVersionedModule(ModuleRevisionId moduleRevisionId) {
        return JkVersionedModule.of(
                JkModuleId.of(moduleRevisionId.getOrganisation(), moduleRevisionId.getName()),
                JkVersion.name(moduleRevisionId.getRevision()));
    }

    // see
    // http://www.draconianoverlord.com/2010/07/18/publishing-to-maven-repos-with-ivy.html
    private static DependencyResolver toResolver(JkRepo repo, Set<String> digesterAlgorithms, boolean download) {
        if (repo instanceof JkRepo.JkMavenRepository) {
            if (!isFileSystem(repo.url()) || download) {
                return ibiblioResolver(repo, digesterAlgorithms);
            }
            return mavenFileSystemResolver(repo, digesterAlgorithms);
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
            result.setChangingPattern("\\*-SNAPSHOT");
            result.setCheckmodified(true);
            if (!digesterAlgorithms.isEmpty()) {
                result.setChecksums(JkUtilsString.join(ivyNameAlgos(digesterAlgorithms), ","));
            }
            return result;
        }

        throw new IllegalStateException(repo + " not handled by translator.");
    }

    private static IBiblioResolver ibiblioResolver(JkRepo repo, Set<String> digesterAlgorithms) {
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
        result.setChangingPattern("\\*-SNAPSHOT");
        result.setCheckmodified(true);
        if (!digesterAlgorithms.isEmpty()) {
            result.setChecksums(JkUtilsString.join(ivyNameAlgos(digesterAlgorithms), ","));
        }
        return result;
    }

    private static FileSystemResolver mavenFileSystemResolver(JkRepo repo, Set<String> digesterAlgorithms) {
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

    static void populateIvySettingsWithRepo(IvySettings ivySettings, JkRepos repos) {
        final DependencyResolver resolver = toChainResolver(repos);
        resolver.setName(MAIN_RESOLVER_NAME);
        ivySettings.addResolver(resolver);
        ivySettings.setDefaultResolver(MAIN_RESOLVER_NAME);
    }

    static void populateIvySettingsWithPublishRepo(IvySettings ivySettings,
            JkPublishRepos repos) {
        for (final JkPublishRepo repo : repos) {
            final DependencyResolver resolver = toResolver(repo.repo(), repo.checksumAlgorithms(), false);
            resolver.setName(PUBLISH_RESOLVER_NAME + repo.repo().url());
            ivySettings.addResolver(resolver);
        }
    }

    static String publishResolverUrl(DependencyResolver resolver) {
        return resolver.getName().substring(PUBLISH_RESOLVER_NAME.length());
    }

    static List<RepositoryResolver> publishResolverOf(IvySettings ivySettings) {
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
            final DependencyResolver resolver = toResolver(jkRepo, Collections.EMPTY_SET, true);
            resolver.setName(jkRepo.toString());
            chainResolver.add(resolver);
        }
        return chainResolver;
    }

    static JkVersionedModule toJkVersionedModule(Artifact artifact) {
        final JkModuleId moduleId = JkModuleId.of(artifact.getModuleRevisionId().getOrganisation(),
                artifact.getModuleRevisionId().getName());
        return JkVersionedModule.of(moduleId,
                JkVersion.name(artifact.getModuleRevisionId().getRevision()));
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
            JkVersionProvider resolvedVersions, IvySettings ivySettings) {

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
        final DependenciesContainer dependencyContainer = new DependenciesContainer(defaultMapping);
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency.dependency();
                final JkVersion resolvedVersion = resolvedVersions.versionOf(externalModule.moduleId());
                dependencyContainer.populate(scopedDependency, resolvedVersion);
            }
        }
        for (final DependencyDescriptor dependencyDescriptor : dependencyContainer.toDependencyDescriptors()) {

            // If we don't set parent, force version on resolution won't work
            final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
            JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
            moduleDescriptor.addDependency(dependencyDescriptor);
        }

        // -- Add dependency exclusion
        for (final JkDepExclude exclude : dependencies.excludes()) {
            final DefaultExcludeRule rule = toExcludeRule(exclude, Arrays.asList(moduleDescriptor.getConfigurationsNames()));
            moduleDescriptor.addExcludeRule(rule);
        }

        // -- Add version override for transitive dependency
        for (final JkModuleId moduleId : resolvedVersions.moduleIds()) {
            final JkVersion version = resolvedVersions.versionOf(moduleId);
            moduleDescriptor.addDependencyDescriptorMediator(toModuleId(moduleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new OverrideDependencyDescriptorMediator(null, version.name()));
        }

        /* Add conflic manager
        LatestRevisionStrategy latestRevisionStrategy = new LatestRevisionStrategy();
        LatestCompatibleConflictManager conflictManager = new LatestCompatibleConflictManager();
        conflictManager.setSettings(ivySettings);
        moduleDescriptor.addConflictManager(ModuleId.newInstance("*", "*"),
                ExactOrRegexpPatternMatcher.INSTANCE, conflictManager);
         */

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

    static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
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

    static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
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
            populateDescriptorWithMavenArtifact(descriptor, classifier, mavenArtifact);
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

    static Artifact toPublishedArtifact(JkIvyPublication.Artifact artifact,
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
        final Map<String, String> extraMap;
        if (classifier == null) {
            extraMap = new HashMap<String, String>();
        } else {
            extraMap = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        }
        return new DefaultArtifact(moduleId, date, artifactName, extension, extension, extraMap);
    }

    static Set<JkScope> toJkScopes(String... confs) {
        final Set<JkScope> scopes = new HashSet<JkScope>();
        for (final String conf : confs) {
            scopes.add(JkScope.of(conf));
        }
        return scopes;
    }

    private static class DependencyDefinition {

        Set<Conf> confs = new HashSet<Conf>();

        JkVersionRange revision;

        List<ArtifactDef> artifacts = new LinkedList<ArtifactDef>();

        boolean includeMainArtifact = false;

        boolean transitive = true;

        List<JkDepExclude> excludes = new LinkedList<JkDepExclude>();

        @SuppressWarnings("rawtypes")
        DefaultDependencyDescriptor toDescriptor(JkModuleId moduleId) {
            final ModuleRevisionId moduleRevisionId = toModuleRevisionId(moduleId, revision);
            final boolean changing = revision.definition().endsWith("-SNAPSHOT");
            final boolean forceVersion = !revision.isDynamic();
            final DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null,
                    moduleRevisionId, forceVersion, changing, transitive);
            for (final Conf conf : confs) {
                result.addDependencyConfiguration(conf.masterConf, conf.depConf);
            }
            for (final ArtifactDef artifactDef : artifacts) {
                final String extension = JkUtilsObject.firstNonNull(artifactDef.type, DEFAULT_EXTENSION);
                final Map<String, String> extra = new HashMap<String, String>();
                if (artifactDef.name != null) {
                    extra.put("classifier", artifactDef.name);
                }
                final DependencyArtifactDescriptor artifactDescriptor = new DefaultDependencyArtifactDescriptor(
                        result,
                        moduleId.name(),
                        extension,
                        extension,
                        null,
                        extra
                        );
                if (artifactDef.confs.isEmpty()) {
                    result.addDependencyArtifact("*", artifactDescriptor);
                } else {
                    for (final String masterConf : artifactDef.confs) {
                        result.addDependencyArtifact(masterConf, artifactDescriptor);
                    }
                }

            }
            if (!artifacts.isEmpty() && includeMainArtifact) {
                final DependencyArtifactDescriptor artifactDescriptor = new DefaultDependencyArtifactDescriptor(
                        result,
                        moduleId.name(),
                        DEFAULT_EXTENSION,
                        DEFAULT_EXTENSION,
                        null,
                        new HashMap()
                        );
                if (confs.isEmpty()) {
                    result.addDependencyArtifact("*", artifactDescriptor);
                } else {
                    for (final Conf conf : confs) {
                        result.addDependencyArtifact(conf.masterConf, artifactDescriptor);
                    }
                }
            }
            for (final JkDepExclude depExclude : excludes) {
                result.addExcludeRule("*", toExcludeRule(depExclude, new LinkedList<String>()));
            }
            return result;

        }

    }

    private static class DependenciesContainer {

        private final Map<JkModuleId, DependencyDefinition> definitions = new LinkedHashMap<JkModuleId, DependencyDefinition>();

        private final JkScopeMapping defaultMapping;

        DependenciesContainer(JkScopeMapping defaultMapping) {
            this.defaultMapping = defaultMapping;
        }

        void populate(JkScopedDependency scopedDependency, JkVersion resolvedVersion) {

            final JkModuleDependency moduleDep = (JkModuleDependency) scopedDependency.dependency();
            final JkModuleId moduleId = moduleDep.moduleId();
            final boolean mainArtifact = moduleDep.classifier() == null && moduleDep.ext() == null;
            this.put(moduleId, moduleDep.transitive(), moduleDep.versionRange(), mainArtifact);

            // fill configuration
            final List<Conf> confs = new LinkedList<Conf>();
            if (scopedDependency.scopeType() == ScopeType.UNSET) {
                if (defaultMapping == null || defaultMapping.entries().isEmpty()) {
                    confs.add(new Conf("*", "*"));
                } else {
                    for (final JkScope entryScope : defaultMapping.entries()) {
                        for (final JkScope mappedScope : defaultMapping.mappedScopes(entryScope)) {
                            confs.add(new Conf(entryScope.name(), mappedScope.name()));
                        }
                    }
                }
            }
            else if (scopedDependency.scopeType() == ScopeType.SIMPLE) {
                for (final JkScope scope : scopedDependency.scopes()) {
                    final JkScopeMapping mapping = resolveSimple(scope, defaultMapping);
                    for (final JkScope fromScope : mapping.entries()) {
                        for (final JkScope mappedScope : mapping.mappedScopes(fromScope)) {
                            confs.add(new Conf(fromScope.name(), mappedScope.name()));
                        }
                    }

                }
            } else if (scopedDependency.scopeType() == ScopeType.MAPPED) {
                for (final JkScope scope : scopedDependency.scopeMapping().entries()) {
                    for (final JkScope mappedScope : scopedDependency.scopeMapping()
                            .mappedScopes(scope)) {
                        confs.add(new Conf(scope.name(), mappedScope.name()));
                    }
                }
            } else {
                if (defaultMapping != null) {
                    for (final JkScope entryScope : defaultMapping.entries()) {
                        for (final JkScope mappedScope : defaultMapping.mappedScopes(entryScope)) {
                            confs.add(new Conf(entryScope.name(), mappedScope.name()));
                        }
                    }
                }
            }
            final Set<String> masterConfs = new HashSet<String>();
            for (final Conf conf : confs) {
                this.addConf(moduleId, conf);
                masterConfs.add(conf.masterConf);
            }
            this.addArtifact(moduleId, masterConfs, moduleDep.classifier(), moduleDep.ext());

            final boolean mainArtifactFlag = moduleDep.classifier() == null && moduleDep.ext() == null;
            this.flagAsMainArtifact(moduleId, mainArtifactFlag);

            // fill artifact exclusion
            for (final JkDepExclude depExclude : moduleDep.excludes()) {
                this.addExludes(moduleId, depExclude);
            }
        }


        private void put(JkModuleId moduleId, boolean transitive, JkVersionRange revision, boolean mainArtifact) {
            DependencyDefinition definition = definitions.get(moduleId);
            if (definition == null) {
                definition = new DependencyDefinition();
                definitions.put(moduleId, definition);
            }

            // if dependency has been declared only once non-transive and once transitive then we consider it has non-transitive
            definition.transitive = definition.transitive && transitive;

            definition.revision = revision;
            definition.includeMainArtifact = definition.includeMainArtifact || mainArtifact;
        }

        private void addConf(JkModuleId moduleId, Conf conf) {
            final DependencyDefinition definition = definitions.get(moduleId);
            definition.confs.add(conf);
        }

        private void addArtifact(JkModuleId moduleId, Set<String> masterConfs, String classifierName, String ext) {
            if (classifierName == null && ext == null) {
                return;
            }
            final DependencyDefinition definition = definitions.get(moduleId);
            definition.artifacts.add(new ArtifactDef(masterConfs, classifierName, ext));
        }

        private void flagAsMainArtifact(JkModuleId moduleId, boolean flag) {
            final DependencyDefinition definition = definitions.get(moduleId);
            definition.includeMainArtifact = definition.includeMainArtifact || flag;
        }

        private void addExludes(JkModuleId moduleId, JkDepExclude depExclude) {
            final DependencyDefinition definition = definitions.get(moduleId);
            definition.excludes.add(depExclude);
        }

        List<DependencyDescriptor> toDependencyDescriptors() {
            final List<DependencyDescriptor> result = new LinkedList<DependencyDescriptor>();
            for (final JkModuleId moduleId : this.definitions.keySet()) {
                result.add(this.definitions.get(moduleId).toDescriptor(moduleId));
            }
            return result;
        }

    }

    private static class Conf {

        private final String masterConf;
        private final String depConf;

        Conf(String masterConf, String depConf) {
            this.masterConf = masterConf;
            this.depConf = depConf;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Conf conf = (Conf) o;
            if (!masterConf.equals(conf.masterConf)) {
                return false;
            }
            return depConf.equals(conf.depConf);
        }

        @Override
        public int hashCode() {
            int result = masterConf.hashCode();
            result = 31 * result + depConf.hashCode();
            return result;
        }
    }

    private static class ArtifactDef {

        final Set<String> confs;
        String name;
        String type;

        ArtifactDef(Set<String> masterConfs, String name, String type) {
            this.confs = masterConfs;
            this.name = name;
            this.type = type;
        }


    }

}
