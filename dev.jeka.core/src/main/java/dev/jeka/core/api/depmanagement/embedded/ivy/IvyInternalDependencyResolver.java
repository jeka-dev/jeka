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
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
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

    private final JkRepoSet repoSet;

    private IvyInternalDependencyResolver(JkRepoSet repoSet) {
        this.repoSet = repoSet;
    }

    static IvyInternalDependencyResolver of(JkRepoSet resolveRepos) {
        return new IvyInternalDependencyResolver(resolveRepos);
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
        final DefaultModuleDescriptor moduleDescriptor = IvyTranslatorToModuleDescriptor.toResolveModuleDescriptor(
                module, deps);
        final String[] confs = moduleDescriptor.getConfigurationsNames();
        final ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setConfs(confs);
        resolveOptions.setTransitive(true);
        resolveOptions.setOutputReport(JkLog.verbosity().isVerbose());
        resolveOptions.setLog(logLevel());
        resolveOptions.setRefresh(parameters.isRefreshed());
        resolveOptions.setCheckIfChanged(true);
        resolveOptions.setOutputReport(true);
        final ResolveReport resolveReport;
        Ivy ivy = IvyTranslatorToIvy.toIvy(repoSet, parameters);
        try {
            resolveReport = ivy.resolve(moduleDescriptor, resolveOptions);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        final JkResolveResult.JkErrorReport errorReport;
        if (resolveReport.hasError()) {
            errorReport = JkResolveResult.JkErrorReport.failure(problems(resolveReport));
        } else {
            errorReport = JkResolveResult.JkErrorReport.allFine();
        }
        final ArtifactDownloadReport[] artifactDownloadReports = resolveReport.getAllArtifactsReports();
        final IvyArtifactContainer artifactContainer = IvyArtifactContainer.of(artifactDownloadReports);
        final JkResolveResult resolveResult = getResolveConf(resolveReport.getDependencies(), module,
                errorReport, artifactContainer);
        if (moduleArg == null) {
            deleteResolveCache(module, ivy);
        }
        return resolveResult;
    }

    @Override
    public File get(JkModuleDependency dependency) {
        final ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency
                .toModuleRevisionId(dependency.toVersionedModule());
        JkModuleDependency.JkArtifactSpecification artifactSpecification =
                dependency.getArtifactSpecifications().isEmpty() ? JkModuleDependency.JkArtifactSpecification.MAIN :
                        dependency.getArtifactSpecifications().iterator().next();
        final DefaultArtifact artifact;
        String type = JkUtilsObject.firstNonNull(artifactSpecification.getType(), "jar");
        if ("pom".equals(artifactSpecification.getType())) {
            artifact = new DefaultArtifact(moduleRevisionId, null, dependency.getModuleId().getName(), type,
                    type, true);
        } else {
            String classifier = artifactSpecification.getClassifier();
            final Map<String, String> extra = new HashMap<>();
            if (classifier != null) {
                extra.put("classifier", classifier);
            }
            artifact = new DefaultArtifact(moduleRevisionId, null, dependency.getModuleId().getName(), type,
                    type, extra);
        }
        Ivy ivy = IvyTranslatorToIvy.toIvy(repoSet, JkResolutionParameters.of());
        final ArtifactDownloadReport report = ivy.getResolveEngine().download(artifact, new DownloadOptions());
        return report.getLocalFile();
    }

    @Override
    public List<String> searchGroups() {
        Ivy ivy = IvyTranslatorToIvy.toIvy(repoSet, JkResolutionParameters.of());
        SearchEngine searchEngine = new SearchEngine(ivy.getSettings());
        return Arrays.asList(searchEngine.listOrganisations()).stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> searchModules(String groupId) {
        Ivy ivy = IvyTranslatorToIvy.toIvy(repoSet, JkResolutionParameters.of());
        SearchEngine searchEngine = new SearchEngine(ivy.getSettings());
        return Arrays.asList(searchEngine.listModules(groupId)).stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> searchVersions(JkModuleId moduleId) {
        Ivy ivy = IvyTranslatorToIvy.toIvy(repoSet, JkResolutionParameters.of());
        SearchEngine searchEngine = new SearchEngine(ivy.getSettings());
        return Arrays.asList(searchEngine.listRevisions(moduleId.getGroup(), moduleId.getName())).stream()
                .sorted()
                .collect(Collectors.toList());
    }


    private void deleteResolveCache(JkVersionedModule module, Ivy ivy) {
        final ResolutionCacheManager cacheManager = ivy.getSettings().getResolutionCacheManager();
        final ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency.toModuleRevisionId(module);
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
                final Set<String> rootConfigurations = JkUtilsIterable.setOf(node.getRootModuleConfigurations());

                List<Path> artifacts;
                if (!node.isCompletelyEvicted()) {
                    artifacts = artifactContainer.getArtifacts(moduleId.withVersion(resolvedVersion.getValue()));
                } else {
                    artifacts = new LinkedList<>();
                }

                final Caller[] callers = node.getAllCallers();
                for (final Caller caller : callers) {
                    final JkVersionedModule parent = IvyTranslatorToDependency
                            .toJkVersionedModule(caller.getModuleRevisionId());
                    final List<JkModuleNodeInfo> list = parentChildMap.computeIfAbsent(
                            parent.getModuleId(), k -> new LinkedList<>());
                    final DependencyDescriptor dependencyDescriptor = caller.getDependencyDescriptor();
                    final Set<String> masterConfigurations = JkUtilsIterable.setOf(
                            dependencyDescriptor.getModuleConfigurations());
                    final JkVersion version = JkVersion.of(dependencyDescriptor
                            .getDynamicConstraintDependencyRevisionId().getRevision());
                    final JkModuleNodeInfo moduleNodeInfo  = JkModuleNodeInfo.of(moduleId, version,
                            masterConfigurations, rootConfigurations, resolvedVersion, artifacts);
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

    private List<JkModuleDepProblem> problems(ResolveReport resolveReport) {
        List<JkModuleDepProblem> result = new LinkedList<>();
        for (String configuration : resolveReport.getConfigurations()) {
            ConfigurationResolveReport configurationResolveReport = resolveReport.getConfigurationReport(configuration);
            for (ArtifactDownloadReport failedArtifactReport : configurationResolveReport.getFailedArtifactsReports()) {
                Artifact artifact = failedArtifactReport.getArtifact();
                ModuleRevisionId moduleRevisionId = artifact.getModuleRevisionId();
                JkVersionedModule versionedModule = IvyTranslatorToDependency.toJkVersionedModule(moduleRevisionId);
                String text = failedArtifactReport.getName() + " : " + failedArtifactReport.getDownloadDetails();
                result.add(JkModuleDepProblem.of(versionedModule, text));
            }
        }
        result.addAll(moduleProblems(resolveReport.getDependencies()));
        return result;
    }

    private List<JkModuleDepProblem> moduleProblems(List<IvyNode> ivyNodes) {
        final List<JkModuleDepProblem> result = new LinkedList<>();
        for (final IvyNode ivyNode : ivyNodes) {
            if (ivyNode.isCompletelyBlacklisted() || ivyNode.isCompletelyEvicted()) {
                continue;
            }
            if (ivyNode.hasProblem()) {
                JkVersionedModule versionedModule = IvyTranslatorToDependency.toJkVersionedModule(ivyNode.getId());
                String text = ivyNode.getProblemMessage();
                JkModuleDepProblem moduleDepProblem = JkModuleDepProblem.of(versionedModule, text);
                result.add(moduleDepProblem);
            }
        }
        return result;
    }

}
