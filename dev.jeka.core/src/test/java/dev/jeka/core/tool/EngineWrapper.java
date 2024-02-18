package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.ide.EclipseKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.builtins.tooling.nexus.NexusKBean;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class EngineWrapper {

    final Class<? extends KBean>[] localKBeanClasses;

    EngineBase engineBase;

    private EngineWrapper(Class<? extends KBean>[] localKBeanClasses) {
        this.localKBeanClasses = localKBeanClasses;
    }

    @SafeVarargs
    static EngineWrapper of(Class<? extends KBean>... localKBeans) {
        return new EngineWrapper(localKBeans);
    }

    EngineWrapper run(String... args) {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-engine-base-");
        EnvLogSettings logSettings = Environment.createLogSettings(args);
        EnvBehaviorSettings behaviorSettings = Environment.createBehaviorSettings(args);
        engineBase = EngineBase.of(baseDir, JkRepoSet.ofLocal(),
                JkDependencySet.of(), logSettings, behaviorSettings);
        EngineBase.KBeanResolution kBeanResolution = kBeanResolution(engineBase, localKBeanClasses);
        run(engineBase, kBeanResolution, args);
        return this;
    }

    EngineWrapper cleanDir() {
        JkPathTree.of(engineBase.baseDir).deleteRoot();
        return this;
    }

    <T extends KBean> Optional<T> find(Class<T> kbeanClass) {
        return engineBase.getRunbase().find(kbeanClass);
    }

    <T extends KBean> T load(Class<T> kbeanClass) {
        return engineBase.getRunbase().load(kbeanClass);
    }

    private static void run(EngineBase engineBase, EngineBase.KBeanResolution kBeanResolution, String[] args) {
        engineBase.resolveClassPaths();
        engineBase.setKBeanResolution(kBeanResolution);
        ParsedCmdLine parsedCmdLine = ParsedCmdLine.parse(args);
        engineBase.resolveEngineCommand(parsedCmdLine.getBeanActions());
        engineBase.run();
    }

    private static EngineBase.KBeanResolution kBeanResolution(EngineBase engineBase,
                                                              Class<? extends KBean>... localKBeanClasses) {
        List<Class<? extends KBean>> allKBeanClasses = new LinkedList<>();
        allKBeanClasses.add(BaseKBean.class);
        allKBeanClasses.add(ProjectKBean.class);
        allKBeanClasses.add(MavenKBean.class);
        allKBeanClasses.add(GitKBean.class);
        allKBeanClasses.add(DockerKBean.class);
        allKBeanClasses.add(IntellijKBean.class);
        allKBeanClasses.add(EclipseKBean.class);
        allKBeanClasses.add(NexusKBean.class);
        allKBeanClasses.addAll(Arrays.asList(localKBeanClasses));
        List<String> allKBeans = allKBeanClasses.stream().map(Class::getName).collect(Collectors.toList());
        List<String> localKBeans = Arrays.stream(localKBeanClasses).map(Class::getName).collect(Collectors.toList());

        EngineBase.DefaultAndInitKBean defaultAndInitKBean = engineBase.defaultAndInitKbean(allKBeans, localKBeans);

        return new EngineBase.KBeanResolution(
                allKBeans, localKBeans, defaultAndInitKBean.initKbeanClassName,
                defaultAndInitKBean.defaultKBeanClassName);
    }
}
