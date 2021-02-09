package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.*;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode.JkModuleNodeInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.search.SearchEngine;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jerome Angibaud
 */
final class IvyInternalDependencyResolver implements JkInternalDependencyResolver {

    private static final Random RANDOM = new Random();

    private static final String[] IVY_24_ALL_CONF = new String[] { "*(public)" };

    private static final String COMPILE = "compile";

    private static final String RUNTIME ="runtime";

    private final Ivy ivy;

    private IvyInternalDependencyResolver(Ivy ivy) {
        super();
        this.ivy = ivy;
    }

    static IvyInternalDependencyResolver of(JkRepoSet resolveRepos) {
        return new IvyInternalDependencyResolver(IvyTranslatorToIvy.toIvy(resolveRepos));
    }

    @Override
    public JkResolveResult resolve(JkVersionedModule moduleArg, JkQualifiedDependencies deps,
                                   JkResolutionParameters parameters) {
        final JkVersionedModule module;
        if (moduleArg == null) {
            module = anonymousVersionedModule();
        } else {
            module = moduleArg;
        }
        final DefaultModuleDescriptor moduleDescriptor = IvyTranslatorToModuleDescriptor.toResolutionModuleDescriptor(
                module, deps, parameters, this.ivy.getSettings());
        final String[] confs = new String[] {"*"};
        final ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setConfs(confs);
        resolveOptions.setTransitive(true);
        resolveOptions.setOutputReport(JkLog.verbosity().isVerbose());
        resolveOptions.setLog(logLevel());
        resolveOptions.setRefresh(parameters.isRefreshed());
        resolveOptions.setCheckIfChanged(true);
        resolveOptions.setOutputReport(true);
        final ResolveReport ivyReport;
        try {
            ivyReport = ivy.resolve(moduleDescriptor, resolveOptions);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        final JkResolveResult.JkErrorReport errorReport;
        if (ivyReport.hasError()) {
            errorReport = JkResolveResult.JkErrorReport.failure(moduleProblems(
                    ivyReport.getDependencies()));
        } else {
            errorReport = JkResolveResult.JkErrorReport.allFine();
        }
        final ArtifactDownloadReport[] artifactDownloadReports = ivyReport.getAllArtifactsReports();
        final IvyArtifactContainer artifactContainer = IvyArtifactContainer.of(artifactDownloadReports);
        final JkResolveResult resolveResult = getResolveConf(ivyReport.getDependencies(), module,
                errorReport, artifactContainer);
        if (moduleArg == null) {
            deleteResolveCache(module);
        }
        return resolveResult;
    }

    @Override
    public File get(JkModuleDependency dependency) {
        final ModuleRevisionId moduleRevisionId = IvyTranslations.toModuleRevisionId(dependency.getModuleId(),
                dependency.getVersion());
        final boolean isMetadata = "pom".equalsIgnoreCase(dependency.getExt());
        final String typeAndExt = JkUtilsObject.firstNonNull(dependency.getExt(), "jar");
        final DefaultArtifact artifact;
        if (isMetadata) {
            artifact = new DefaultArtifact(moduleRevisionId, null, dependency.getModuleId().getName(), typeAndExt,
                    typeAndExt, true);
        } else {
            final Map<String, String> extra = new HashMap<>();
            if (dependency.getClassifier() != null) {
                extra.put("classifier", dependency.getClassifier());
            }
            artifact = new DefaultArtifact(moduleRevisionId, null, dependency.getModuleId().getName(), typeAndExt,
                    typeAndExt, extra);
        }
        final ArtifactDownloadReport report = ivy.getResolveEngine().download(artifact, new DownloadOptions());
        return report.getLocalFile();
    }

    @Override
    public List<String> searchGroups() {
        SearchEngine searchEngine = new SearchEngine(this.ivy.getSettings());
        return Arrays.asList(searchEngine.listOrganisations()).stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> searchModules(String groupId) {
        SearchEngine searchEngine = new SearchEngine(this.ivy.getSettings());
        return Arrays.asList(searchEngine.listModules(groupId)).stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> searchVersions(JkModuleId moduleId) {
        SearchEngine searchEngine = new SearchEngine(this.ivy.getSettings());
        return Arrays.asList(searchEngine.listRevisions(moduleId.getGroup(), moduleId.getName())).stream()
                .sorted()
                .collect(Collectors.toList());
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
        if (JkLog.Verbosity.MUTE == JkLog.verbosity()) {
            return "quiet";
        }
        if (JkLog.Verbosity.VERBOSE == JkLog.verbosity()) {
            return "verbose";
        }
        return "download-only";
    }

    private static JkResolveResult getResolveConf(List<IvyNode> nodes,
            JkVersionedModule rootVersionedModule,
            JkResolveResult.JkErrorReport errorReport,
            IvyArtifactContainer ivyArtifactContainer) {

        // Compute dependency tree
        final JkResolvedDependencyNode tree = createTree(nodes, rootVersionedModule, ivyArtifactContainer);
        return JkResolveResult.of(tree, errorReport);
    }

    private static JkVersionedModule anonymousVersionedModule() {
        final String version = Long.toString(RANDOM.nextLong());
        return JkVersionedModule.of(JkModuleId.of("anonymousGroup", "anonymousName"), JkVersion.of(version));
    }


    private static JkResolvedDependencyNode createTree(Iterable<IvyNode> nodes, JkVersionedModule rootVersionedModule,
                                                       IvyArtifactContainer artifactContainer) {
        final IvyTreeResolver treeResolver = new IvyTreeResolver(nodes, artifactContainer);
        final JkResolvedDependencyNode.JkModuleNodeInfo treeRootNodeInfo = JkResolvedDependencyNode.JkModuleNodeInfo.ofRoot(rootVersionedModule);
        return treeResolver.createNode(treeRootNodeInfo);
    }

    private static class IvyTreeResolver {

        // parent to children parentChildMap
        private final Map<JkModuleId, List<JkModuleNodeInfo>> parentChildMap = new HashMap<>();

        IvyTreeResolver(Iterable<IvyNode> nodes, IvyArtifactContainer artifactContainer) {


            for (final IvyNode node : nodes) {
                if (node.isCompletelyBlacklisted()) {
                    continue;
                }
                final JkModuleId moduleId = JkModuleId.of(node.getId().getOrganisation(), node.getId().getName());
                final JkVersion resolvedVersion = JkVersion.of(node.getResolvedId().getRevision());
                final Set<String> rootScopes = JkUtilsIterable.setOf(node.getRootModuleConfigurations());

                List<Path> artifacts;
                if (!node.isCompletelyEvicted()) {
                    artifacts = artifactContainer.getArtifacts(moduleId.withVersion(resolvedVersion.getValue()));
                } else {
                    artifacts = new LinkedList<>();
                }

                final Caller[] callers = node.getAllCallers();
                for (final Caller caller : callers) {
                    final JkVersionedModule parent = IvyTranslations.toJkVersionedModule(caller.getModuleRevisionId());
                    final List<JkModuleNodeInfo> list = parentChildMap.computeIfAbsent(parent.getModuleId(), k -> new LinkedList<>());
                    final DependencyDescriptor dependencyDescriptor = caller.getDependencyDescriptor();
                    final Set<String> declaredScopes = JkUtilsIterable.setOf(
                            dependencyDescriptor.getModuleConfigurations());
                    final JkVersion version = JkVersion.of(dependencyDescriptor
                            .getDynamicConstraintDependencyRevisionId().getRevision());
                    final JkModuleNodeInfo moduleNodeInfo  = JkModuleNodeInfo.of(moduleId, version, declaredScopes,
                            rootScopes, resolvedVersion, artifacts);
                    if (!containSame(list, moduleId)) {
                        list.add(moduleNodeInfo);
                    }
                }
            }
        }

        private static boolean containSame(List<JkModuleNodeInfo> list, JkModuleId moduleId) {
            for (final JkModuleNodeInfo moduleNodeInfo : list) {
                if (moduleNodeInfo.getModuleId().equals(moduleId)) {
                    return true;
                }
            }
            return false;
        }

        JkResolvedDependencyNode createNode(JkModuleNodeInfo holder) {
            if (parentChildMap.get(holder.getModuleId()) == null || holder.isEvicted()) {
                return JkResolvedDependencyNode.ofModuleDep(holder, new LinkedList<>());
            }

            List<JkResolvedDependencyNode.JkModuleNodeInfo> moduleNodeInfos = parentChildMap.get(holder.getModuleId());
            if (moduleNodeInfos == null) {
                moduleNodeInfos = new LinkedList<>();
            }
            final List<JkResolvedDependencyNode> childNodes = new LinkedList<>();
            for (final JkModuleNodeInfo moduleNodeInfo : moduleNodeInfos) {
                final JkResolvedDependencyNode childNode = createNode(moduleNodeInfo);
                childNodes.add(childNode);
            }
            return JkResolvedDependencyNode.ofModuleDep(holder, childNodes);
        }
    }

    private List<JkModuleDepProblem> moduleProblems(List<IvyNode> ivyNodes) {
        final List<JkModuleDepProblem> result = new LinkedList<>();
        for (final IvyNode ivyNode : ivyNodes) {
            if (ivyNode.isCompletelyBlacklisted() || ivyNode.isCompletelyEvicted()) {
                continue;
            }
            if (ivyNode.hasProblem()) {
                final JkModuleId jkModuleId = JkModuleId.of(ivyNode.getModuleId().getOrganisation(), ivyNode.getModuleId().getName());
                final JkModuleDepProblem problem = JkModuleDepProblem.of(jkModuleId,
                        ivyNode.getId().getRevision(),
                        ivyNode.getProblemMessage());
                result.add(problem);
            }
        }
        return result;
    }

}
