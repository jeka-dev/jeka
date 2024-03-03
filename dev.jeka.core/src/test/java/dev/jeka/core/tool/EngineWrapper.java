package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProperties;
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

    Engine engine;

    @SafeVarargs
    EngineWrapper(Class<? extends KBean> ...jekaSrcKBeanClasses) {
        this.jekaSrcKBeanClasses = jekaSrcKBeanClasses;
    }

    EngineWrapper run(String ...args) {
        return run(JkProperties.EMPTY, args);
    }

    EngineWrapper run(JkProperties props, String... args) {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-engine-base-");

        CmdLineArgs cmdArgs = new CmdLineArgs(args);
        PicocliMainCommand mainCommand = new PicocliMainCommand();
        CommandLine cmdLine = new CommandLine(mainCommand);
        cmdLine.parseArgs(cmdArgs.withOptionsOnly().get());
        LogSettings logSettings = mainCommand.logSettings();
        BehaviorSettings behaviorSettings = mainCommand.behaviorSettings();

        engine = Engine.of(baseDir, false, JkRepoSet.ofLocal(),
                JkDependencySet.of(), logSettings, behaviorSettings);
        Engine.KBeanResolution kBeanResolution = kBeanResolution(engine, jekaSrcKBeanClasses);
        run(engine, kBeanResolution, props, args);
        return this;
    }

    EngineWrapper cleanDir() {
        JkPathTree.of(engine.baseDir).deleteRoot();
        return this;
    }

    <T extends KBean> Optional<T> find(Class<T> kbeanClass) {
        return engine.getRunbase().find(kbeanClass);
    }

    <T extends KBean> T load(Class<T> kbeanClass) {
        return engine.getRunbase().load(kbeanClass);
    }

    private void run(Engine engine, Engine.KBeanResolution kBeanResolution, JkProperties props, String[] args) {
        engine.resolveClassPaths();
        engine.setKBeanResolution(kBeanResolution);
        KBeanAction.Container cmdLineActions = parse(args, props, kBeanResolution);
        props.getAllStartingWith("", true).forEach(System::setProperty);
        try {
            engine.initRunbase(cmdLineActions);
        } finally {
            props.getAllStartingWith("", true).keySet().forEach(System::clearProperty);
        }
        engine.run();
    }

    @SafeVarargs
    private static Engine.KBeanResolution kBeanResolution(Engine engine,
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

        Engine.DefaultAndInitKBean defaultAndInitKBean = engine.defaultAndInitKbean(allKBeans, jekaSrcKBeans);

        return new Engine.KBeanResolution(
                allKBeans, jekaSrcKBeans, defaultAndInitKBean.initKbeanClassName,
                defaultAndInitKBean.defaultKBeanClassName);
    }

    private KBeanAction.Container parse(String[] args, JkProperties props, Engine.KBeanResolution kBeanResolution) {
        return PicocliParser.parse(new CmdLineArgs(args), kBeanResolution);
    }
}
