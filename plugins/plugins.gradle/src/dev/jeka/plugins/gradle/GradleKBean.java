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

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@JkDoc("Wraps Gradle build commands from JeKa.")
public class GradleKBean extends KBean {

    @JkDoc("Arguments to pass to the gradle command. Example: 'clean build', '--info -Pprofile=prod'")
    public String args = "";

    @JkDoc("In multi-module projects, the module containing the application to build. " +
            "Defaults to root module if not specified.")
    public String appModule;

    @JkDoc("Runs Gradle from JeKa using the arguments from 'args' field.")
    public void wrap() {
        doWrap(JkGradleProject.of(getBaseDir()));
    }

    @JkDoc("Calls Gradle 'build' (skipping tests) from JeKa and copies produced artifacts from build/libs/ to jeka-output.")
    public void wrapBuild() {
        JkGradleProject gradleRootModule = JkGradleProject.of(getBaseDir());
        JkGradleProject gradleAppModule = gradleRootModule;
        if (!JkUtilsString.isBlank(appModule)) {
            gradleAppModule = JkGradleProject.of(getBaseDir().resolve(appModule));
        }
        doWrap(gradleRootModule, "clean", "build", "-x", "test");
        var outputFilesTree = JkPathTree.of(gradleAppModule.getBuildLibsDir()).andMatching("*");
        if (!outputFilesTree.containFiles()) {
            throw new IllegalStateException("No output files found in " + gradleAppModule.getBuildLibsDir() +
                    " after Gradle build execution. Check Gradle configuration.");
        }
        outputFilesTree.getFiles().stream()
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    JkLog.verbose("Copying file %s to %s", file, getOutputDir());
                    JkPathFile.of(file).copyToDir(this.getOutputDir(), StandardCopyOption.REPLACE_EXISTING);
                });
    }

    @JkDoc("Calls Gradle 'nativeCompile' (skipping tests) from JeKa and copies the native executable from " +
            "build/native/nativeCompile/ to jeka-output. Requires GraalVM as the current JDK and the " +
            "org.graalvm.buildtools.native plugin.")
    public void wrapNative() {
        JkGradleProject gradleRootModule = JkGradleProject.of(getBaseDir());
        JkGradleProject gradleAppModule = gradleRootModule;
        if (!JkUtilsString.isBlank(appModule)) {
            gradleAppModule = JkGradleProject.of(getBaseDir().resolve(appModule));
        }
        doWrap(gradleRootModule, "nativeCompile", "-x", "test");
        var outputFilesTree = JkPathTree.of(gradleAppModule.getNativeCompileDir()).andMatching("*");
        if (!outputFilesTree.containFiles()) {
            throw new IllegalStateException("No output files found in " + gradleAppModule.getNativeCompileDir() +
                    " after Gradle nativeCompile. Check GraalVM and org.graalvm.buildtools.native configuration.");
        }
        outputFilesTree.getFiles().stream()
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    JkLog.verbose("Copying file %s to %s", file, getOutputDir());
                    JkPathFile.of(file).copyToDir(this.getOutputDir(), StandardCopyOption.REPLACE_EXISTING);
                });
    }

    private void doWrap(JkGradleProject gradleProject, String... arguments) {
        String newPath = JkJavaProcess.CURRENT_JAVA_EXEC_DIR + File.pathSeparator + System.getenv("PATH");
        gradleProject.gradle()
                .setEnv("PATH", newPath)
                .setEnv("JAVA_HOME", JkJavaProcess.CURRENT_JAVA_HOME.toString())
                .setEnv("GRAALVM_HOME", JkJavaProcess.CURRENT_JAVA_HOME.toString())
                .addParams(arguments)
                .addParamsAsCmdLine(args)
                .exec();
    }

}
