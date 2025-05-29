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

package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

class AotPreProcessor {

    private final List<Path> classpath;

    private final String mainClass;

    private final JkModuleId moduleId;

    private  final Path generatedSourcesDir;
    private final Path generatedResourcesDir;
    private final Path generatedClassesDir;

    public final List<String> profiles = new LinkedList<>();

    public final List<String> aotRunArguments = new LinkedList<>();

    AotPreProcessor(List<Path> classpath, String mainClass, Path outputDir, JkModuleId moduleId) {
        this.classpath = classpath;
        this.mainClass = mainClass;
        this.moduleId = moduleId;
        this.generatedSourcesDir = outputDir.resolve("generated-sources");
        this.generatedResourcesDir = outputDir.resolve("generated-resources");
        this.generatedClassesDir = outputDir.resolve("generated-classes");
    }

    static AotPreProcessor of(JkBuildable buildable) {
        JkModuleId moduleId1 = Optional.ofNullable(buildable.getModuleId())
                .orElse(JkModuleId.of(
                        buildable.getBaseDir().toAbsolutePath().getFileName().toString(),
                        buildable.getBaseDir().toAbsolutePath().getFileName().toString()));
        List<Path> classpath = new LinkedList<>();
        classpath.add(buildable.getClassDir());
        classpath.addAll(buildable.getRuntimeDependenciesAsFiles());
        String mainClass = buildable.getMainClass();
        Path outputDir = buildable.getOutputDir().resolve("spring-aot");
        return new AotPreProcessor(classpath, mainClass, outputDir, moduleId1);
    }

    void generate() {
        JkLog.info("Run Spring AOT processor tool");
        JkJavaProcess.ofJava("org.springframework.boot.SpringApplicationAotProcessor")
                .setClasspath(classpath)
                .addParams(getAotArguments())
                .setLogCommand(JkLog.isDebug())
                .setLogWithJekaDecorator(JkLog.isVerbose())
                .setInheritIO(false)
                .exec();
    }

    public Path getGeneratedSourcesDir() {
        return generatedSourcesDir;
    }

    public Path getGeneratedResourcesDir() {
        return generatedResourcesDir;
    }

    public Path getGeneratedClassesDir() {
        return generatedClassesDir;
    }

    public List<Path> getClasspath() {
        return classpath;
    }

    private List<String> getAotArguments() {
        List<String> aotArguments = new LinkedList<>();
        aotArguments.add(mainClass);
        aotArguments.add(generatedSourcesDir.toString());
        aotArguments.add(generatedResourcesDir.toString());
        aotArguments.add(generatedClassesDir.toString());
        aotArguments.add(moduleId.getGroup());
        aotArguments.add(moduleId.getName());
        aotArguments.addAll(resolveArguments());
        return aotArguments;
    }

    private List<String> resolveArguments() {
        List<String> runArguments = new LinkedList<>(aotRunArguments);
        if (!profiles.isEmpty()) {
            runArguments.add(0, "--spring.profiles.active=" + String.join(",", this.profiles));
        }
        return runArguments;
    }
}
