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

package dev.jeka.plugins.gradle;

import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a Gradle project with utility methods for performing common tasks.
 */
public class JkGradleProject {

    public final Path baseDir;

    private JkGradleProject(Path baseDir) {
        JkUtilsAssert.state(
                Files.exists(baseDir.resolve("build.gradle")) || Files.exists(baseDir.resolve("build.gradle.kts")),
                "No build.gradle or build.gradle.kts file found at %s. Can't process.", baseDir);
        this.baseDir = baseDir;
    }

    public static JkGradleProject of(Path baseDir) {
        return new JkGradleProject(baseDir);
    }

    /**
     * Returns a new JkGradle instance to perform a <i>build</i>.
     */
    public JkGradle build() {
        return gradle().addParams("build");
    }

    /**
     * Returns a new JkGradle instance to perform a <i>clean build</i>.
     */
    public JkGradle cleanBuild() {
        return gradle().addParams("clean", "build");
    }

    /**
     * Returns a new JkGradle instance to perform a <i>publishToMavenLocal</i>.
     * This is the Gradle equivalent of Maven's <i>install</i> goal.
     */
    public JkGradle publishToMavenLocal() {
        return gradle().addParams("publishToMavenLocal");
    }

    /**
     * Returns a new instance of {@link JkGradle} to perform Gradle commands on this project.
     */
    public JkGradle gradle() {
        return JkGradle.of(baseDir);
    }

    /**
     * Returns the directory where Gradle places produced artifacts (jars, wars, etc.).
     */
    public Path getBuildLibsDir() {
        return baseDir.resolve("build").resolve("libs");
    }

    /**
     * Returns the Gradle build output directory.
     */
    public Path getBuildDir() {
        return baseDir.resolve("build");
    }

    /**
     * Returns the directory where GraalVM Native Build Tools places the compiled native executable.
     */
    public Path getNativeCompileDir() {
        return baseDir.resolve("build").resolve("native").resolve("nativeCompile");
    }

}
