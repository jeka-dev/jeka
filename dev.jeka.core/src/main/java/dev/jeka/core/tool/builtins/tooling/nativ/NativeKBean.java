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

package dev.jeka.core.tool.builtins.tooling.nativ;

import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@JkDoc("Creates native images (experimental !)\n" +
        "A native images is an executable file created from Java bytecode.\n" +
        "This KBean allows to create native images from executable jars generated from the project.")
public class NativeKBean extends KBean {

    @JkDoc("Creates an native image from the main artifact jar of the project.\n" +
            "If no artifact found, a build is triggered by invoking 'JkProject.packaging.createFatJar(mainArtifactPath)'.")
    public void build() {
        Path javaHome = JkJavaProcess.CURRENT_JAVA_HOME;
        JkProject project = load(ProjectKBean.class).project;
        Path jar = project.artifactLocator.getMainArtifactPath();
        if (!Files.exists(jar)) {
            project.packaging.createFatJar(jar);
        }
        boolean hasMessageBundle = false;
        try (JkZipTree jarTree = JkZipTree.of(jar)) {
            Path resourceBundle = jarTree.get("MessagesBundle.properties");
            if (Files.exists(resourceBundle)) {
                hasMessageBundle = true;
                JkLog.verbose("Found MessagesBundle to include in the native image.");
            }
        }
        String relTarget = JkUtilsString.substringBeforeLast(jar.toString(), ".jar");
        Path target = Paths.get(relTarget).toAbsolutePath();
        Path nativeImageExe = JkUtilsSystem.IS_WINDOWS ?
                javaHome.resolve("bin/native-image.cmd") :
                javaHome.resolve("bin/native-image");
        JkProcess.of(nativeImageExe.toString(), "--no-fallback")
                .addParams("-H:+UnlockExperimentalVMOptions")
                .addParams("-H:IncludeResources=.*(?<!\\.class)")
                .addParamsIf(hasMessageBundle, "-H:IncludeResourceBundles=MessagesBundle")
                .addParams("-jar", jar.toString())
                .addParams("-H:Name=" + target)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .exec();
        JkLog.info("Generated in %s", target);
        JkLog.info("Run: %s", relTarget);
    }

}
