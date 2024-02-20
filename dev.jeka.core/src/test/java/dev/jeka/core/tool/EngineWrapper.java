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

    final Class<? extends KBean>[] jekaSrcKBeanClasses;

    EngineBase engineBase;

    @SafeVarargs
    EngineWrapper(Class<? extends KBean> ...jekaSrcKBeanClasses) {
        this.jekaSrcKBeanClasses = jekaSrcKBeanClasses;
    }

    EngineWrapper run(String... args) {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-engine-base-");
        EnvLogSettings logSettings = Environment.createLogSettings(args);
        EnvBehaviorSettings behaviorSettings = Environment.createBehaviorSettings(args);
        engineBase = EngineBase.of(baseDir, false, JkRepoSet.ofLocal(),
                JkDependencySet.of(), logSettings, behaviorSettings);
        EngineBase.KBeanResolution kBeanResolution = kBeanResolution(engineBase, jekaSrcKBeanClasses);
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

    private void run(EngineBase engineBase, EngineBase.KBeanResolution kBeanResolution, String[] args) {
        engineBase.resolveClassPaths();
        engineBase.setKBeanResolution(kBeanResolution);
        List<KBeanAction> kBeanActions = parse(args, kBeanResolution);
        engineBase.resolveEngineCommand(kBeanActions);
        engineBase.run();
    }

    @SafeVarargs
    private static EngineBase.KBeanResolution kBeanResolution(EngineBase engineBase,
                                                              Class<? extends KBean>... jekaSrcKBeanClasses) {
        List<Class<? extends KBean>> allKBeanClasses = new LinkedList<>();
        allKBeanClasses.add(BaseKBean.class);
        allKBeanClasses.add(ProjectKBean.class);
        allKBeanClasses.add(MavenKBean.class);
        allKBeanClasses.add(GitKBean.class);
        allKBeanClasses.add(DockerKBean.class);
        allKBeanClasses.add(IntellijKBean.class);
        allKBeanClasses.add(EclipseKBean.class);
        allKBeanClasses.add(NexusKBean.class);
        allKBeanClasses.addAll(Arrays.asList(jekaSrcKBeanClasses));
        List<String> allKBeans = allKBeanClasses.stream().map(Class::getName).collect(Collectors.toList());
        List<String> jekaSrcKBeans = Arrays.stream(jekaSrcKBeanClasses).map(Class::getName).collect(Collectors.toList());

        EngineBase.DefaultAndInitKBean defaultAndInitKBean = engineBase.defaultAndInitKbean(allKBeans, jekaSrcKBeans);

        return new EngineBase.KBeanResolution(
                allKBeans, jekaSrcKBeans, defaultAndInitKBean.initKbeanClassName,
                defaultAndInitKBean.defaultKBeanClassName);
    }

    private List<KBeanAction> parse(String[] args, EngineBase.KBeanResolution kBeanResolution) {
        return PicocliParser.parse(new CmdLineArgs(args), kBeanResolution);
    }
}
