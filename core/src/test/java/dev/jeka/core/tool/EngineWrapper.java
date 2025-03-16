/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
        LogSettings.INSTANCE = mainCommand.logSettings();
        BehaviorSettings.INSTANCE = mainCommand.behaviorSettings();

        engine = Engine.of(true, baseDir, JkRepoSet.ofLocal(), JkDependencySet.of());
        KBeanResolution kBeanResolution = kBeanResolution(engine, jekaSrcKBeanClasses);
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

    private void run(Engine engine, KBeanResolution kBeanResolution, JkProperties props, String[] args) {
        engine.resolveClassPaths();
        engine.setKBeanResolution(kBeanResolution);
        KBeanAction.Container cmdLineActions = parse(args, props, kBeanResolution);
        props.getAllStartingWith("", true).forEach(System::setProperty);
        try {
            engine.getOrCreateRunbase(cmdLineActions);
        } finally {
            props.getAllStartingWith("", true).keySet().forEach(System::clearProperty);
        }
        engine.run();
    }

    @SafeVarargs
    private static KBeanResolution kBeanResolution(Engine engine,
                                                   Class<? extends KBean>... jekaSrcKBeanClasses) {
        List<Class<? extends KBean>> allKBeanClasses = new LinkedList<>();
        allKBeanClasses.add(BaseKBean.class);
        allKBeanClasses.add(ProjectKBean.class);
        allKBeanClasses.add(MavenKBean.class);
        allKBeanClasses.add(GitKBean.class);
        allKBeanClasses.add(DockerKBean.class);
        allKBeanClasses.add(IntellijKBean.class);
        allKBeanClasses.add(EclipseKBean.class);
        allKBeanClasses.addAll(Arrays.asList(jekaSrcKBeanClasses));
        List<String> allKBeans = allKBeanClasses.stream().map(Class::getName).collect(Collectors.toList());
        List<String> jekaSrcKBeans = Arrays.stream(jekaSrcKBeanClasses).map(Class::getName).collect(Collectors.toList());
        String defaultKBeanClassName = engine.defaultKBeanClassName(allKBeans, jekaSrcKBeans);

        return new KBeanResolution(allKBeans, jekaSrcKBeans, defaultKBeanClassName);
    }

    private KBeanAction.Container parse(String[] args, JkProperties props, KBeanResolution kBeanResolution) {
        return CmdLineParser.parse(new CmdLineArgs(args), kBeanResolution);
    }
}
