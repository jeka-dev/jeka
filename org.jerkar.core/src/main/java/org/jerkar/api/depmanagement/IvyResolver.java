package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.*;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;

/**
 * Jerkar users : This class is not part of the public API !!! Please, Use
 * {@link JkDependencyResolver} instead. Ivy wrapper providing high level methods. The
 * API is expressed using Jerkar classes only (mostly free of Ivy classes).
 *
 * @author Jerome Angibaud
 */
final class IvyResolver implements InternalDepResolver {

    private static final Random RANDOM = new Random();

    private static final String[] IVY_24_ALL_CONF = new String[] { "*(public)" };

    private final Ivy ivy;

    private IvyResolver(Ivy ivy) {
        super();
        this.ivy = ivy;
    }

    private static IvyResolver of(IvySettings ivySettings) {
        final Ivy ivy = ivy(ivySettings);
        return new IvyResolver(ivy);
    }

    static Ivy ivy(IvySettings ivySettings) {
        final Ivy ivy = new Ivy();
        ivy.getLoggerEngine().popLogger();
        ivy.getLoggerEngine().setDefaultLogger(new IvyMessageLogger());
        ivy.getLoggerEngine().setShowProgress(JkLog.verbose());
        ivy.getLoggerEngine().clearProblems();

        //if (IvyContext.getContext().peekIvy() == null) {
        IvyContext.getContext().setIvy(ivy);
        //}
        ivy.setSettings(ivySettings);
        ivy.bind();
        URLHandlerRegistry.setDefault(new IvyFollowRedirectUrlHandler());
        return ivy;
    }

    /**
     * Creates an <code>IvySettings</code> from the specified repositories.
     */
    private static IvySettings ivySettingsOf(JkRepos resolveRepos) {
        final IvySettings ivySettings = new IvySettings();
        IvyTranslations.populateIvySettingsWithRepo(ivySettings, resolveRepos);
        ivySettings.setDefaultCache(JkLocator.jerkarRepositoryCache());
        return ivySettings;
    }

    /**
     * Creates an instance using specified repository for publishing and the
     * specified repositories for resolving.
     */
    public static IvyResolver of(JkRepos resolveRepos) {
        return of(ivySettingsOf(resolveRepos));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JkResolveResult resolve(JkVersionedModule moduleArg, JkDependencies deps,
            JkResolutionParameters parameters, JkVersionProvider versionProvider, JkScope ... resolvedScopes) {

        final JkVersionedModule module;
        if (moduleArg == null) {
            module = anonymousVersionedModule();
        } else {
            module = moduleArg;
        }

        if (parameters == null) {
            parameters = JkResolutionParameters.of();
        }
        if (versionProvider == null) {
            versionProvider = JkVersionProvider.empty();
        }
        final DefaultModuleDescriptor moduleDescriptor = IvyTranslations.toPublicationLessModule(module, deps,
                parameters.defaultMapping(), versionProvider);

        final String[] confs = toConfs(deps.declaredScopes(), resolvedScopes);
        final ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setConfs(confs);
        resolveOptions.setTransitive(true);
        resolveOptions.setOutputReport(JkLog.verbose());
        resolveOptions.setLog(logLevel());
        resolveOptions.setRefresh(parameters.refreshed());
        resolveOptions.setCheckIfChanged(true);
        if (resolvedScopes.length == 0) {   // if no scope, verbose ivy report turns in exception
            resolveOptions.setOutputReport(false);
        }
        final ResolveReport report;
        try {
            report = ivy.resolve(moduleDescriptor, resolveOptions);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        final JkResolveResult.JkErrorReport errorReport;
        if (report.hasError()) {
            errorReport = JkResolveResult.JkErrorReport.failure(missingArtifacts(
                    report.getAllArtifactsReports()));
        } else {
            errorReport = JkResolveResult.JkErrorReport.allFine();
        }
        final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
        JkResolveResult resolveResult = JkResolveResult.empty(module);
        final String[] allConfs = resolveOptions.getConfs(moduleDescriptor);
        for (final String conf : allConfs) {
            final JkResolveResult confResult = getResolveConf(conf, artifactDownloadReports, deps,
                    report.getDependencies(), module, errorReport);
            resolveResult = resolveResult.and(confResult);
        }
        if (moduleArg == null) { // remob
            deleteResolveCache(module);
        }
        return resolveResult;
    }

    private void deleteResolveCache(JkVersionedModule module) {
        final ResolutionCacheManager cacheManager = this.ivy.getSettings().getResolutionCacheManager();
        final ModuleRevisionId moduleRevisionId = IvyTranslations.toModuleRevisionId(module);
        final File propsFile = cacheManager.getResolvedIvyPropertiesInCache(moduleRevisionId);
        propsFile.delete();
        final File xmlFile = cacheManager.getResolvedIvyFileInCache(moduleRevisionId);
        xmlFile.delete();
    }

    private static String logLevel() {
        if (JkLog.silent()) {
            return "quiet";
        }
        if (JkLog.verbose()) {
            return "verbose";
        }
        return "download-only";
    }

    private static JkResolveResult getResolveConf(String config, ArtifactDownloadReport[] artifactDownloadReports,
                                                  JkDependencies deps, List<IvyNode> nodes,
                                                  JkVersionedModule rootVersionedModule,
                                                  JkResolveResult.JkErrorReport errorReport) {

        // Get module dependency files
        final List<JkModuleDepFile> moduleDepFiles = new LinkedList<JkModuleDepFile>();
        JkVersionProvider versionProvider = JkVersionProvider.empty();
        for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
            final JkVersionedModule versionedModule = IvyTranslations
                    .toJkVersionedModule(artifactDownloadReport.getArtifact());
            final JkModuleDepFile moduleDepFile = JkModuleDepFile.of(versionedModule,
                    artifactDownloadReport.getLocalFile());
            moduleDepFiles.add(moduleDepFile);
            final JkScopedDependency declaredDep = deps.get(versionedModule.moduleId());
            if (declaredDep != null && declaredDep.isInvolvedIn(JkScope.of(config))) {
                final JkModuleDependency module = (JkModuleDependency) declaredDep.dependency();
                if (module.versionRange().isDynamicAndResovable()) {
                    versionProvider = versionProvider.and(module.moduleId(), versionedModule.version());
                }
            }
        }

        // Compute dependency tree
        final JkDependencyNode tree = createTree(nodes, config, rootVersionedModule, deps);
        return JkResolveResult.of(moduleDepFiles, versionProvider, tree, errorReport);
    }

    private static JkVersionedModule anonymousVersionedModule() {
        final String version = Long.toString(RANDOM.nextLong());
        return JkVersionedModule.of(JkModuleId.of("anonymousGroup", "anonymousName"), JkVersion.name(version));
    }

    @Override
    public File get(JkModuleDependency dependency) {
        final ModuleRevisionId moduleRevisionId = IvyTranslations.toModuleRevisionId(dependency.moduleId(),
                dependency.versionRange());
        final boolean isMetadata = "pom".equalsIgnoreCase(dependency.ext());
        final String typeAndExt = JkUtilsObject.firstNonNull(dependency.ext(), "jar");
        final DefaultArtifact artifact;
        if (isMetadata) {
            artifact = new DefaultArtifact(moduleRevisionId, null, dependency.moduleId().name(), typeAndExt,
                    typeAndExt, true);
        } else {
            Map<String, String> extra = new HashMap<String, String>();
            if (dependency.classifier() != null) {
                extra.put("classifier", dependency.classifier());
            }
            artifact = new DefaultArtifact(moduleRevisionId, null, dependency.moduleId().name(), typeAndExt,
                    typeAndExt, extra);
        }
        final ArtifactDownloadReport report = ivy.getResolveEngine().download(artifact, new DownloadOptions());
        return report.getLocalFile();
    }

    private static JkDependencyNode createTree(Iterable<IvyNode> nodes, String config, JkVersionedModule rootVersionedModule, JkDependencies deps) {
        final TreeResolver treeResolver = new TreeResolver(deps);
        treeResolver.populate(nodes, config);
        final JkModuleDependency moduleDependency = JkModuleDependency.of(rootVersionedModule);
        final JkScopedDependency scopedDependency = JkScopedDependency.of(moduleDependency, toScopes(config));
        return treeResolver.createNode(scopedDependency);
    }

    private static class TreeResolver {

        // parent to children map
        private final Map<JkModuleId, List<JkScopedDependency>> map = new HashMap<JkModuleId, List<JkScopedDependency>>();

        private final JkDependencies firstLevelDeps;

        TreeResolver(JkDependencies deps) {
            this.firstLevelDeps = deps;
        }

        void populate(Iterable<IvyNode> nodes, String confs) {
            for (final IvyNode node : nodes) {
                if (node.isEvicted(confs)) {
                    continue;
                }
                final JkVersionedModule currentModule = toJkVersionedModule(node);
                final JkDependency currentDep = JkModuleDependency.of(currentModule);
                final Set<JkScope> scopes;
                if (this.firstLevelDeps.get(currentModule.moduleId()) != null) {
                    scopes = this.firstLevelDeps.get(currentModule.moduleId()).scopes();
                } else {
                    scopes = IvyTranslations.toJkScopes(node.getRequiredConfigurations());
                }

                final JkScopedDependency scopedDependency = JkScopedDependency.of(currentDep, scopes);

                final Caller[] callers = node.getAllCallers();
                for (final Caller caller : callers) {
                    final JkVersionedModule parent = IvyTranslations.toJkVersionedModule(caller.getModuleRevisionId());
                    List<JkScopedDependency> list = map.get(parent.moduleId());
                    if (list == null) {
                        list = new LinkedList<JkScopedDependency>();
                        map.put(parent.moduleId(), list);
                    }
                    list.add(scopedDependency);
                }

            }
        }

        JkDependencyNode createNode(JkScopedDependency dep) {
            final JkModuleDependency moduleDependency = (JkModuleDependency) dep.dependency();
            final JkModuleId moduleId = moduleDependency.moduleId();
            if (map.get(moduleId) == null) {
                return new JkDependencyNode(dep, new LinkedList<JkDependencyNode>());
            }
            List<JkScopedDependency> modules = map.get(moduleId);
            if (modules == null) {
                modules = new LinkedList<JkScopedDependency>();
            }
            final List<JkDependencyNode> childNodes = new LinkedList<JkDependencyNode>();
            for (final JkScopedDependency scopedDep : modules) {
                final JkDependencyNode childNode = createNode(scopedDep);
                if (!containsSameModuleId(childNodes, childNode.root())) {
                    childNodes.add(childNode);
                }
            }
            return new JkDependencyNode(dep, childNodes);
        }

        private static boolean containsSameModuleId(List<JkDependencyNode> dependencies, JkScopedDependency dependency) {
            for (final JkDependencyNode node : dependencies) {
                if (moduleId(node.root()).equals(moduleId(dependency))) {
                    return true;
                }
            }
            return false;
        }

        private static JkModuleId moduleId(JkScopedDependency scopedDependency) {
            final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency.dependency();
            return moduleDependency.moduleId();
        }

    }

    private static JkVersionedModule toJkVersionedModule(IvyNode ivyNode) {
        return IvyTranslations.toJkVersionedModule(ivyNode.getResolvedId());
    }

    private static Set<JkScope> toScopes(String configParam) {
        final String[] configs = JkUtilsString.split(configParam, ",");
        final Set<JkScope> result = new HashSet<JkScope>();
        for (final String config : configs) {
            result.add(JkScope.of(config.trim()));
        }
        return result;
    }

    private List<JkArtifactDef> missingArtifacts(ArtifactDownloadReport[] artifactDownloadReports) {
        List<JkArtifactDef> result = new LinkedList<JkArtifactDef>();
        for (ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
            if (artifactDownloadReport.getDownloadStatus() == DownloadStatus.FAILED) {
                if (ArtifactDownloadReport.MISSING_ARTIFACT.equals(artifactDownloadReport.getDownloadDetails())) {
                    JkArtifactDef artifactDef = IvyTranslations.toJkArtifactDef((MDArtifact) artifactDownloadReport.getArtifact());
                    result.add(artifactDef);
                }
            }
        }
        return result;
    }

    private String[] toConfs(Set<JkScope> declaredScopes, JkScope ... resolvedScopes) {
        if (resolvedScopes.length == 0) {
            return IVY_24_ALL_CONF;
        }
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < resolvedScopes.length; i++) {
            List<JkScope> scopes = resolvedScopes[i].commonScopes(declaredScopes);
            for (JkScope scope : scopes) {
                result.add(scope.name());
            }
        }
        return JkUtilsIterable.arrayOf(result, String.class);
    }

}
