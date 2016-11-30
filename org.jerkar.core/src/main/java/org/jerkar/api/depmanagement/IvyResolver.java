package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsThrowable;

/**
 * Jerkar users : This class is not part of the public API !!! Please, Use
 * {@link JkPublisher} instead. Ivy wrapper providing high level methods. The
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

    private static InternalDepResolver of(IvySettings ivySettings) {
        final Ivy ivy = ivy(ivySettings);
        return new IvyResolver(ivy);
    }

    static Ivy ivy(IvySettings ivySettings) {
        final Ivy ivy = new Ivy();
        ivy.getLoggerEngine().setDefaultLogger(new IvyMessageLogger());
        ivy.getLoggerEngine().setShowProgress(JkLog.verbose());
        if (IvyContext.getContext().peekIvy() == null) {
            IvyContext.getContext().setIvy(ivy);
        }
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
    public static InternalDepResolver of(JkRepos resolveRepos) {
        return of(ivySettingsOf(resolveRepos));
    }

    @Override
    public JkResolveResult resolveAnonymous(JkDependencies deps, JkScope resolvedScope,
            JkResolutionParameters parameters, JkVersionProvider tranditiveVersionOverride) {
        final JkVersionedModule anonymous = anonymousVersionedModule();
        final JkResolveResult result = resolve(anonymous, deps, resolvedScope, parameters, tranditiveVersionOverride);
        deleteResolveCache(anonymous);
        return result;
    }

    @Override
    public JkResolveResult resolve(JkVersionedModule module, JkDependencies deps, JkScope resolvedScope,
            JkResolutionParameters parameters, JkVersionProvider tranditiveVersionOverride) {

        final DefaultModuleDescriptor moduleDescriptor = IvyTranslations.toPublicationLessModule(module, deps,
                parameters.defaultMapping(), tranditiveVersionOverride);

        final String[] confs = resolvedScope == null ? IVY_24_ALL_CONF : new String[] { resolvedScope.name() };
        final ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setConfs(confs);
        resolveOptions.setTransitive(true);
        resolveOptions.setOutputReport(JkLog.verbose());
        resolveOptions.setLog(logLevel());
        resolveOptions.setRefresh(parameters.refreshed());
        resolveOptions.setCheckIfChanged(true);
        final ResolveReport report;
        try {
            report = ivy.resolve(moduleDescriptor, resolveOptions);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        if (report.hasError()) {
            for (final ArtifactDownloadReport artifactDownloadReport : report.getAllArtifactsReports()) {
                JkLog.info(artifactDownloadReport.toString());
            }

            throw new IllegalStateException("Errors while resolving dependencies : " + report.getAllProblemMessages());
        }
        final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
        JkResolveResult resolveResult = JkResolveResult.empty();
        for (final String conf : resolveOptions.getConfs(moduleDescriptor)) {
            final JkResolveResult confResult = getResolveConf(conf, artifactDownloadReports, deps);
            resolveResult = resolveResult.and(confResult);
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

    @Override
    public JkAttachedArtifacts getArtifacts(Iterable<JkVersionedModule> modules, JkScope... scopes) {
        final JkVersionedModule anonymous = anonymousVersionedModule();
        final DefaultModuleDescriptor moduleDescriptor = IvyTranslations.toUnpublished(anonymous);
        for (final JkScope jkScope : scopes) {
            moduleDescriptor.addConfiguration(new Configuration(jkScope.name()));
        }
        for (final JkVersionedModule module : modules) {
            final ModuleRevisionId moduleRevisionId = IvyTranslations.toModuleRevisionId(module);
            final DefaultDependencyDescriptor dependency = new DefaultDependencyDescriptor(moduleRevisionId, true,
                    false);
            for (final JkScope scope : scopes) {
                dependency.addDependencyConfiguration(scope.name(), scope.name());
            }
            moduleDescriptor.addDependency(dependency);
        }
        final JkAttachedArtifacts result = new JkAttachedArtifacts();
        final ResolveOptions resolveOptions = new ResolveOptions().setTransitive(false).setOutputReport(JkLog.verbose())
                .setRefresh(false);
        resolveOptions.setLog(logLevel());
        for (final JkScope scope : scopes) {
            resolveOptions.setConfs(IvyTranslations.toConfNames(scope));
            final ResolveReport report;
            try {
                report = ivy.resolve(moduleDescriptor, resolveOptions);
            } catch (final Exception e1) {
                throw new RuntimeException(e1);
            }
            final ArtifactDownloadReport[] artifactDownloadReports = report.getAllArtifactsReports();
            for (final ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
                final JkVersionedModule versionedModule = IvyTranslations
                        .toJkVersionedModule(artifactDownloadReport.getArtifact());
                final JkModuleDepFile artifact = JkModuleDepFile.of(versionedModule,
                        artifactDownloadReport.getLocalFile());
                result.add(scope, artifact);
            }
        }
        deleteResolveCache(anonymous);
        return result;
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
            JkDependencies deps) {
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
        return JkResolveResult.of(moduleDepFiles, versionProvider);
    }

    static JkVersionedModule anonymousVersionedModule() {
        final String version = Long.toString(RANDOM.nextLong());
        return JkVersionedModule.of(JkModuleId.of("anonymousGroup", "anonymousName"), JkVersion.ofName(version));
    }

    @Override
    public File get(JkModuleDependency dependency) {
        final ModuleRevisionId moduleRevisionId = IvyTranslations.toModuleRevisionId(dependency, null);
        final boolean metadata = "pom".equalsIgnoreCase(dependency.ext());
        final String typeAndExt = JkUtilsObject.firstNonNull(dependency.ext(), "jar");
        final Artifact artifact = new DefaultArtifact(moduleRevisionId, null, dependency.moduleId().name(), typeAndExt,
                typeAndExt, metadata);
        final ArtifactDownloadReport report = ivy.getResolveEngine().download(artifact, new DownloadOptions());
        return report.getLocalFile();
    }

    private static class TreeResolver {

        private final Map<JkModuleId, List<JkVersionedModule>> map = new HashMap<JkModuleId, List<JkVersionedModule>>();

        void populate(Iterable<IvyNode> nodes, String confs) {
            for (final IvyNode node : nodes) {
                if (node.isEvicted(confs)) {
                    continue;
                }
                final JkVersionedModule currentModule = toJkVersionedModule(node);
                final Caller[] callers = node.getAllCallers();
                for (int i = 0; i <= callers.length; i++) {
                    final Caller caller = callers[i];
                    final JkVersionedModule parent = IvyTranslations.toJkVersionedModule(caller.getModuleRevisionId());
                    List<JkVersionedModule> list = map.get(parent.moduleId());
                    if (list == null) {
                        list = new LinkedList<JkVersionedModule>();
                        map.put(parent.moduleId(), list);
                    }
                    list.add(currentModule);
                }

            }
        }

    }

    private static JkVersionedModule toJkVersionedModule(IvyNode ivyNode) {
        return IvyTranslations.toJkVersionedModule(ivyNode.getId());
    }

}
