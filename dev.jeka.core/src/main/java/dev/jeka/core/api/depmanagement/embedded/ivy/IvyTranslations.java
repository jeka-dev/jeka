package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMapping;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMappingSet;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.descriptor.Configuration.Visibility;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.*;
import org.apache.ivy.util.url.CredentialsStore;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

final class IvyTranslations {

    private static final String MAIN_RESOLVER_NAME = "MAIN";

    private static final String EXTRA_NAMESPACE = "http://ant.apache.org/ivy/extra";

    private static final String EXTRA_PREFIX = "e";

    private static final String PUBLISH_RESOLVER_NAME = "publisher:";

    private static final String MAVEN_ARTIFACT_PATTERN = "/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

    private static final Configuration DEFAULT_CONFIGURATION = new Configuration("default");

    private static final String DEFAULT_EXTENSION = "jar";

    private IvyTranslations() {
    }

    static DefaultModuleDescriptor toResolutionModuleDescriptor(JkVersionedModule module,
                                                                  .Jk dependencies,
                                                                  JkVersionProvider resolvedVersions) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .getModuleId().getGroup(), module.getModuleId().getName(), module.getVersion().getValue());
        final DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(
                thisModuleRevisionId, "integration", null);

        populateModuleDescriptorForResolution(moduleDescriptor, dependencies, resolvedVersions);
        return moduleDescriptor;
    }

    private static void populateModuleDescriptorForResolution(DefaultModuleDescriptor moduleDescriptor,
                                                 JkDependencySet dependencies,
                                                 JkVersionProvider resolvedVersions) {

        // Add dependencies
        JkIvyConfigurationMappingSet defaultConfigMappling = JkIvyConfigurationMappingSet.of();
        DependenciesContainer dependencyContainer = new DependenciesContainer(defaultConfigMappling, dependencies);
        for (JkModuleDependency moduleDependency : dependencies.getModuleDependencies()) {
            String targetConf = JkTransitivity.COMPILE == moduleDependency.getTransitivity() ?
                    "compile" : "runtime";
            JkIvyConfigurationMapping depConfMapping = JkIvyConfigurationMappingSetof("*",
                    JkIvyConfigurationMappingSet.ARCHIVE_MASTER, targetConf + "(default)");
            dependencyContainer.populate(moduleDependency, depConfMapping);
        }
        for (final DependencyDescriptor dependencyDescriptor : dependencyContainer.toDependencyDescriptors()) {

            // If we don't set parent, force version on resolution won't work
            final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
            JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
            moduleDescriptor.addDependency(dependencyDescriptor);
        }

        // -- Add dependency exclusion
        for (final JkDependencyExclusion exclude : dependencies.getGlobalExclusions()) {
            final DefaultExcludeRule rule = toExcludeRule(exclude, Arrays.asList(moduleDescriptor.getConfigurationsNames()));
            moduleDescriptor.addExcludeRule(rule);
        }

        // -- Add version override for transitive dependency
        for (final JkModuleId moduleId : resolvedVersions.getModuleIds()) {
            final JkVersion version = resolvedVersions.getVersionOf(moduleId);
            moduleDescriptor.addDependencyDescriptorMediator(toModuleId(moduleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new OverrideDependencyDescriptorMediator(null, version.getValue()));
        }

    }

    static DefaultModuleDescriptor toPublicationLessModule(JkVersionedModule module,
                                                           JkQualifiedDependencies dependencies,
                                                           JkIvyConfigurationMappingSet defaultMapping,
                                                           JkVersionProvider resolvedVersions) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .getModuleId().getGroup(), module.getModuleId().getName(), module.getVersion().getValue());
        final DefaultModuleDescriptor result = new DefaultModuleDescriptor(
                thisModuleRevisionId, "integration", null);
        populateModuleDescriptor(result, dependencies, defaultMapping, resolvedVersions);
        return result;
    }



    private static DefaultExcludeRule toExcludeRule(JkDependencyExclusion depExclude, Iterable<String> allRootConfs) {
        final String type = depExclude.getClassifier() == null ? PatternMatcher.ANY_EXPRESSION : depExclude
                .getClassifier();
        final String ext = depExclude.getExtension() == null ? PatternMatcher.ANY_EXPRESSION : depExclude
                .getExtension();
        final ArtifactId artifactId = new ArtifactId(toModuleId(depExclude.getModuleId()), "*", type,
                ext);
        final DefaultExcludeRule result = new DefaultExcludeRule(artifactId,
                ExactPatternMatcher.INSTANCE, null);
        for (final String conf : allRootConfs) {
            result.addConfiguration(conf);
        }
        return result;
    }

    private static Configuration createConfiguration(String configName, String description, boolean transitive,
                                                     String ... extendConf) {
        final List<String> extendedScopes = new LinkedList<>();
        final Visibility visibility = Visibility.PUBLIC;
        return new Configuration(configName, visibility, description, extendConf, transitive, null);
    }

    static ModuleRevisionId toModuleRevisionId(JkModuleId moduleId, JkVersion version) {
        final String originalVersion = version.getValue();
        final Map<String, String> extra = new HashMap<>();
        return ModuleRevisionId.newInstance(moduleId.getGroup(), moduleId.getName(), originalVersion, extra);
    }

    private static ModuleId toModuleId(JkModuleId moduleId) {
        return new ModuleId(moduleId.getGroup(), moduleId.getName());
    }

    static ModuleRevisionId toModuleRevisionId(JkVersionedModule jkVersionedModule) {
        return new ModuleRevisionId(toModuleId(jkVersionedModule.getModuleId()), jkVersionedModule
                .getVersion().getValue());
    }

    static JkVersionedModule toJkVersionedModule(ModuleRevisionId moduleRevisionId) {
        return JkVersionedModule.of(
                JkModuleId.of(moduleRevisionId.getOrganisation(), moduleRevisionId.getName()),
                JkVersion.of(moduleRevisionId.getRevision()));
    }

    // see
    // http://www.draconianoverlord.com/2010/07/18/publishing-to-maven-repos-with-ivy.html
    private static DependencyResolver toResolver(JkRepo repo, boolean download) {
        if (!repo.isIvyRepo()) {
            if (!isFileSystem(repo.getUrl()) || download) {
                return ibiblioResolver(repo);
            }
            return mavenFileSystemResolver(repo);
        }
        final JkRepo.JkRepoIvyConfig ivyRepoConfig = repo.getIvyConfig();
        if (isFileSystem(repo.getUrl())) {
            final FileRepository fileRepo = new FileRepository(new File(repo.getUrl().getPath()));
            final FileSystemResolver result = new FileSystemResolver();
            result.setRepository(fileRepo);
            for (final String pattern : ivyRepoConfig.artifactPatterns()) {
                result.addArtifactPattern(completePattern(repo.getUrl().getPath(), pattern));
            }
            for (final String pattern : ivyRepoConfig.ivyPatterns()) {
                result.addIvyPattern(completePattern(repo.getUrl().getPath(), pattern));
            }
            return result;
        }
        if (repo.getUrl().getProtocol().equals("http") || repo.getUrl().getProtocol().equals("https")) {
            final IvyRepResolver result = new IvyRepResolver();
            result.setIvyroot(repo.getUrl().toString());
            result.setArtroot(repo.getUrl().toString());
            result.setArtpattern(IvyRepResolver.DEFAULT_IVYPATTERN);
            result.setIvypattern(IvyRepResolver.DEFAULT_IVYPATTERN);
            result.setM2compatible(false);
            if (isHttp(repo.getUrl())) {
                final JkRepo.JkRepoCredentials credentials = repo.getCredentials();
                if (!CredentialsStore.INSTANCE.hasCredentials(repo.getUrl().getHost()) && credentials != null) {
                    CredentialsStore.INSTANCE.addCredentials(credentials.getRealm(), repo.getUrl().getHost(),
                            credentials.getUserName(), credentials.getPassword());

                }
            }
            result.setChangingPattern("\\*-SNAPSHOT");
            result.setCheckmodified(true);
            return result;
        }
        throw new IllegalStateException(repo.getUrl() .getProtocol()+ " not handled for translating repo "+ repo);
    }

    private static IBiblioResolver ibiblioResolver(JkRepo repo) {
        final IBiblioResolver result = new IBiblioResolver();
        result.setM2compatible(true);
        result.setUseMavenMetadata(true);
        result.setRoot(repo.getUrl().toString());
        result.setUsepoms(true);
        if (isHttp(repo.getUrl())) {
            final JkRepo.JkRepoCredentials credentials = repo.getCredentials();
            if (!CredentialsStore.INSTANCE.hasCredentials(repo.getUrl().getHost()) && credentials != null) {
                CredentialsStore.INSTANCE.addCredentials(credentials.getRealm(),
                        repo.getUrl().getHost(), credentials.getUserName(), credentials.getPassword());

            }
        }
        result.setChangingPattern("\\*-SNAPSHOT");
        result.setCheckmodified(true);
        return result;
    }

    private static FileSystemResolver mavenFileSystemResolver(JkRepo repo) {
        final FileRepository fileRepo = new FileRepository(new File(repo.getUrl().getPath()));
        final FileSystemResolver result = new FileSystemResolver();
        result.setRepository(fileRepo);
        result.addArtifactPattern(completePattern(repo.getUrl().getPath(), MAVEN_ARTIFACT_PATTERN));
        result.setM2compatible(true);
        return result;
    }

    private static boolean isFileSystem(URL url) {
        return url.getProtocol().equals("file");
    }

    private static boolean isHttp(URL url) {
        return url.getProtocol().equals("http") || url.getProtocol().equals("https");
    }

    static void populateIvySettingsWithRepo(IvySettings ivySettings, JkRepoSet repos) {
        final DependencyResolver resolver = toChainResolver(repos);
        resolver.setName(MAIN_RESOLVER_NAME);
        ivySettings.addResolver(resolver);
        ivySettings.setDefaultResolver(MAIN_RESOLVER_NAME);
    }

    static void populateIvySettingsWithPublishRepo(IvySettings ivySettings,
                                                   JkRepoSet repos) {
        for (final JkRepo publishRepo : repos.getRepoList()) {
            final DependencyResolver resolver = toResolver(publishRepo,false);
            resolver.setName(PUBLISH_RESOLVER_NAME + publishRepo.getUrl());
            ivySettings.addResolver(resolver);
        }
    }

    static String publishResolverUrl(DependencyResolver resolver) {
        return resolver.getName().substring(PUBLISH_RESOLVER_NAME.length());
    }

    static List<RepositoryResolver> publishResolverOf(IvySettings ivySettings) {
        final List<RepositoryResolver> resolvers = new LinkedList<>();
        for (final Object resolverObject : ivySettings.getResolvers()) {
            final RepositoryResolver resolver = (RepositoryResolver) resolverObject;
            if (resolver.getName() != null && resolver.getName().startsWith(PUBLISH_RESOLVER_NAME)) {
                resolvers.add(resolver);
            }
        }
        return resolvers;
    }

    @SuppressWarnings("unchecked")
    private static ChainResolver toChainResolver(JkRepoSet repos) {
        final ChainResolver chainResolver = new ChainResolver();
        for (final JkRepo jkRepo : repos.getRepoList()) {
            final DependencyResolver resolver = toResolver(jkRepo, true);
            resolver.setName(jkRepo.toString());
            chainResolver.add(resolver);
        }
        return chainResolver;
    }


    private static void populateModuleDescriptor(DefaultModuleDescriptor moduleDescriptor,
                                                 JkQualifiedDependencies dependencies,
                                                 JkIvyConfigurationMappingSet defaultMapping,
                                                 JkVersionProvider resolvedVersions) {

        // Add configuration definitions
        for (final JkScope involvedScope : dependencies.getDeclaredScopes()) {
            final Configuration configuration = createConfiguration(involvedScope);
            moduleDescriptor.addConfiguration(configuration);
        }
        if (dependencies.getDeclaredScopes().isEmpty()) {
            moduleDescriptor.addConfiguration(DEFAULT_CONFIGURATION);
        }
        for (final JkScope scope : defaultMapping.getConfigurationMappings()) {
            final Configuration configuration = createConfiguration(scope);
            moduleDescriptor.addConfiguration(configuration);
        }
        moduleDescriptor.setDefaultConfMapping(defaultMapping.toIvyExpression());

        // Add dependencies
        final DependenciesContainer dependencyContainer = new DependenciesContainer(defaultMapping, dependencies);
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                dependencyContainer.populate(scopedDependency);
            }
        }
        for (final DependencyDescriptor dependencyDescriptor : dependencyContainer.toDependencyDescriptors()) {

            // If we don't set parent, force version on resolution won't work
            final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
            JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
            moduleDescriptor.addDependency(dependencyDescriptor);
        }

        // -- Add dependency exclusion
        for (final JkDependencyExclusion exclude : dependencies.getGlobalExclusions()) {
            final DefaultExcludeRule rule = toExcludeRule(exclude, Arrays.asList(moduleDescriptor.getConfigurationsNames()));
            moduleDescriptor.addExcludeRule(rule);
        }

        // -- Add version override for transitive dependency
        for (final JkModuleId moduleId : resolvedVersions.getModuleIds()) {
            final JkVersion version = resolvedVersions.getVersionOf(moduleId);
            moduleDescriptor.addDependencyDescriptorMediator(toModuleId(moduleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new OverrideDependencyDescriptorMediator(null, version.getValue()));
        }

    }


    private static JkIvyConfigurationMappingSet resolveSimple(String scope, JkIvyConfigurationMappingSet defaultMapping) {
        final JkIvyConfigurationMappingSet result;
        if (scope == null) {
            if (defaultMapping == null) {
                result = JkIvyConfigurationMappingSet.of(JkScope.of("default")).to("default");
            } else {
                result = defaultMapping;
            }
        } else {
            if (defaultMapping == null) {
                result = JkIvyConfigurationMappingSet.of(scope).to(scope.getName());
            } else {
                if (defaultMapping.getConfigurationMappings().contains(scope)) {
                    result = JkIvyConfigurationMappingSet.of(scope).to(defaultMapping.getMappedScopes(scope));
                } else {
                    result = scope.mapTo(scope.getName() + "(default)");
                }

            }
        }
        return result;
    }



    private static String completePattern(String url, String pattern) {
        return url + "/" + pattern;
    }


    static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
            JkIvyPublication publication, Instant publishDate) {
        Iterator<JkIvyPublication.JkPublicationArtifact> it = publication.getAllArtifacts().iterator();
        while (it.hasNext()) {
            JkIvyPublication.JkPublicationArtifact artifact = it.next();
            for (final String jkScope : artifact.configuration) {
                if (!Arrays.asList(descriptor.getConfigurations()).contains(jkScope)) {
                    descriptor.addConfiguration(createConfiguration(jkScope));
                }
            }
            final Artifact ivyArtifact = toPublishedArtifact(artifact, descriptor.getModuleRevisionId(), publishDate);
            for (final JkScope jkScope : artifact.jkScopes) {
                descriptor.addArtifact(jkScope.getName(), ivyArtifact);
            }
        }
    }



    static void populateModuleDescriptorWithPublication(DefaultModuleDescriptor descriptor,
                                                        JkMavenPublication publication, Instant publishDate) {
        JkArtifactLocator artifactLocator = publication.getArtifactLocator();
        final ModuleRevisionId moduleRevisionId = descriptor.getModuleRevisionId();
        final String ivyArtifactName = moduleRevisionId.getName();
        final Artifact mavenMainArtifact = toPublishedMavenArtifact(artifactLocator.getMainArtifactPath(),
                ivyArtifactName, null, moduleRevisionId, publishDate);
        final String mainConf = "default";
        populateDescriptorWithMavenArtifact(descriptor, mainConf, mavenMainArtifact);
        for (final JkArtifactId artifactId  : artifactLocator.getArtifactIds()) {
            if (artifactId.equals(artifactLocator.getMainArtifactId())) {
                continue;
            }
            final Path file = artifactLocator.getArtifactPath(artifactId);
            final String classifier = artifactId.getName();
            final Artifact mavenArtifact = toPublishedMavenArtifact(file, ivyArtifactName, classifier,
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

    static Artifact toPublishedArtifact(JkIvyPublication.JkPublicationArtifact artifact,
                                        ModuleRevisionId moduleId, Instant date) {
        final String artifactName = JkUtilsString.isBlank(artifact.name) ? moduleId.getName()
                : artifact.name;
        final String extension = JkUtilsObject.firstNonNull(artifact.extension, "");
        final String type = JkUtilsObject.firstNonNull(artifact.type, extension);
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), artifactName, type, extension);
    }

    private static Artifact toPublishedMavenArtifact(Path artifactFile,
                                                     String classifier, ModuleRevisionId moduleId, Instant date) {
        final String extension = JkUtilsString.substringAfterLast(artifactFile.getFileName().toString(), ".");
        final Map<String, String> extraMap;
        if (JkArtifactId.MAIN_ARTIFACT_NAME.equals(classifier)) {
            extraMap = new HashMap<>();
        } else {
            extraMap = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        }
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), moduleId.getName(), extension, extension, extraMap);
    }



    private static class DependencyDefinition {

        final Set<Conf> confs = new HashSet<>();

        JkVersion version;

        final List<ArtifactDef> artifacts = new LinkedList<>();

        boolean includeMainArtifact = false;

        boolean transitive = true;

        final List<JkDependencyExclusion> excludes = new LinkedList<>();

        @SuppressWarnings("rawtypes")
        DefaultDependencyDescriptor toDescriptor(JkModuleId moduleId) {
            final ModuleRevisionId moduleRevisionId = toModuleRevisionId(moduleId, version);
            final boolean changing = version.getValue().endsWith("-SNAPSHOT");
            final boolean forceVersion = !version.isDynamic();
            final DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null,
                    moduleRevisionId, forceVersion, changing, transitive);
            for (final Conf conf : confs) {
                result.addDependencyConfiguration(conf.masterConf, conf.depConf);
            }
            for (final ArtifactDef artifactDef : artifacts) {
                final String extension = JkUtilsObject.firstNonNull(artifactDef.type, DEFAULT_EXTENSION);
                final Map<String, String> extra = new HashMap<>();
                if (artifactDef.name != null) {
                    extra.put("classifier", artifactDef.name);
                }
                final DependencyArtifactDescriptor artifactDescriptor = new DefaultDependencyArtifactDescriptor(
                        result,
                        moduleId.getName(),
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
                        moduleId.getName(),
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
            for (final JkDependencyExclusion depExclude : excludes) {
                result.addExcludeRule("*", toExcludeRule(depExclude, new LinkedList<>()));
            }
            return result;

        }

    }

    private static class DependenciesContainer {

        private final Map<JkModuleId, DependencyDefinition> definitions = new LinkedHashMap<>();

        private final JkIvyConfigurationMappingSet defaultMapping;

        private final JkDependencySet dependencySet;

        DependenciesContainer(JkIvyConfigurationMappingSet defaultMapping, JkDependencySet dependencySet) {
            this.defaultMapping = defaultMapping;
            this.dependencySet = dependencySet;
        }

        void populate(JkModuleDependency moduleDep, JkIvyConfigurationMapping depConfMapping) {
            final JkModuleId moduleId = moduleDep.getModuleId();
            final boolean isMainArtifact = moduleDep.getClassifier() == null && moduleDep.getExt() == null;
            JkVersion version = dependencySet.getVersion(moduleId);
            boolean transitive = moduleDep.getTransitivity() != JkTransitivity.NONE;
            this.put(moduleId, transitive, version, isMainArtifact);

            //fillConfigurationAndReturnMasterConfs(depConfMapping)
            this.addArtifact(moduleId, new HashSet<>(), moduleDep.getClassifier(), moduleDep.getExt());

            final boolean mainArtifactFlag = moduleDep.getClassifier() == null && moduleDep.getExt() == null;
            this.flagAsMainArtifact(moduleId, mainArtifactFlag);

            // fill artifact exclusion
            for (final JkDependencyExclusion depExclude : moduleDep.getExclusioins()) {
                this.addExludes(moduleId, depExclude);
            }
        }

        private Set<String> fillConfigurationAndReturnMasterConfs(JkIvyConfigurationMappingSet.Entry depConfMapping) {

            // fill configuration
            final List<Conf> confs = new LinkedList<>();
            if (depConfMapping.getFrom().isEmpty()) {
                if (defaultMapping.getConfigurationMappings().isEmpty()) {
                    confs.add(new Conf("*", "*"));
                } else {
                    for (JkIvyConfigurationMappingSet.Entry entry : defaultMapping.getConfigurationMappings()) {
                        for (final String conf : defaultMapping.getMappedScopes(entryScope)) {
                            confs.add(new Conf(entryScope.getName(), mappedScope));
                        }
                    }
                }
            } else if (dependency.getScopeType() == ScopeType.SIMPLE) {
                for (final JkScope scope : dependency.getScopes()) {
                    final JkIvyConfigurationMappingSet mapping = resolveSimple(scope, defaultMapping);
                    for (final JkScope fromScope : mapping.getConfigurationMappings()) {
                        for (final String mappedScope : mapping.getMappedScopes(fromScope)) {
                            confs.add(new Conf(fromScope.getName(), mappedScope));
                        }
                    }
                }
            } else if (dependency.getScopeType() == ScopeType.MAPPED) {
                for (final JkScope scope : dependency.getScopeMapping().getEntries()) {
                    for (final String mappedScope : dependency.getScopeMapping()
                            .getMappedScopes(scope)) {
                        confs.add(new Conf(scope.getName(), mappedScope));
                    }
                }
            } else {
                if (defaultMapping != null) {
                    for (final JkScope entryScope : defaultMapping.getConfigurationMappings()) {
                        for (final String mappedScope : defaultMapping.getMappedScopes(entryScope)) {
                            confs.add(new Conf(entryScope.getName(), mappedScope));
                        }
                    }
                }
            }
            final Set<String> masterConfs = new HashSet<>();
            for (final Conf conf : confs) {
                this.addConf(moduleId, conf);
                masterConfs.add(conf.masterConf);
            }
            return masterConfs;
        }




        private void put(JkModuleId moduleId, boolean transitive, JkVersion version, boolean mainArtifact) {
            final DependencyDefinition definition = definitions.computeIfAbsent(moduleId, k -> new DependencyDefinition());

            // if dependency has been declared only once non-transitive and once transitive then we consider it has non-transitive
            definition.transitive = definition.transitive && transitive;

            definition.version = version;
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

        private void addExludes(JkModuleId moduleId, JkDependencyExclusion depExclude) {
            final DependencyDefinition definition = definitions.get(moduleId);
            definition.excludes.add(depExclude);
        }

        List<DependencyDescriptor> toDependencyDescriptors() {
            final List<DependencyDescriptor> result = new LinkedList<>();
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

        final String name;

        final String type;

        ArtifactDef(Set<String> masterConfs, String name, String type) {
            this.confs = masterConfs;
            this.name = name;
            this.type = type;
        }


    }

}
