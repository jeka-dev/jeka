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

import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

// Use class org.springframework.boot.SpringApplicationAotProcessor
// found in org.springframework.boot:spring-boot

// see:  https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-tools/spring-boot-maven-plugin/src/main/java/org/springframework/boot/maven

class NativeMaker {

    private final JkProject project;

    public final List<String> profiles = new LinkedList<>();

    public final List<String> aotRunArguments = new LinkedList<>();

    NativeMaker(JkProject project) {
        this.project = project;
    }

    void prepareAot() {
        List<Path> classpath = new LinkedList<>();
        classpath.add(project.compilation.layout.resolveClassDir());
        classpath.addAll(project.packaging.resolveRuntimeDependenciesAsFiles());
        JkJavaProcess.ofJava("org.springframework.boot.SpringApplicationAotProcessor")
                .setClasspath(classpath)
                .addParams(getAotArguments())
                .setLogCommand(JkLog.isVerbose())
                .setInheritIO(false)
                .exec();
    }

    private List<String> getAotArguments() {
        List<String> aotArguments = new LinkedList<>();
        String applicationClassName = project.packaging.getMainClass();
        aotArguments.add(applicationClassName);
        Path aotBaseDir = project.getOutputDir().resolve("spring-aot");
        aotArguments.add(aotBaseDir.resolve("generated-sources").toString());
        aotArguments.add(aotBaseDir.resolve("generated-resources").toString());
        aotArguments.add(aotBaseDir.resolve("generated-classes").toString());
        aotArguments.add(this.project.getModuleId().getGroup());
        aotArguments.add(this.project.getModuleId().getName());
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
