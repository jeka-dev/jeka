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

package dev.jeka.core.api.j2e;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JkJ2eWarArchiver {

    private Path classDir;

    private Path webappDir;

    private List<Path> libs;

    private Path extraStaticResourceDir;

    private JkJ2eWarArchiver() {
    }

    public static JkJ2eWarArchiver of() {
        return new JkJ2eWarArchiver();
    }


    public JkJ2eWarArchiver setClassDir(Path classDir) {
        this.classDir = classDir;
        return this;
    }

    public JkJ2eWarArchiver setWebappDir(Path webappDir) {
        this.webappDir = webappDir;
        return this;
    }

    public JkJ2eWarArchiver setLibs(List<Path> libs) {
        this.libs = libs;
        return this;
    }

    public JkJ2eWarArchiver setExtraStaticResourceDir(Path extraStaticResourceDir) {
        this.extraStaticResourceDir = extraStaticResourceDir;
        return this;
    }

    public void generateWarDir(Path destDir) {
        JkPathTree webappTree = webappDir != null ? JkPathTree.of(webappDir) : null;
        if (webappTree == null || !webappTree.exists() || !webappTree.containFiles()) {
            JkLog.warn(destDir + " is empty or does not exists.");
        } else {
            webappTree.copyTo(destDir);
        }
        if (extraStaticResourceDir != null && Files.exists(extraStaticResourceDir)) {
            JkPathTree.of(extraStaticResourceDir).copyTo(destDir);
        }
        JkPathTree.of(classDir).copyTo(destDir.resolve("WEB-INF/classes"));
        Path libDir = destDir.resolve("lib");
        JkPathTree.of(libDir).deleteContent();
        libs.forEach(path -> JkPathFile.of(path).copyToDir(libDir));
    }

    public void generateWarFile(Path destFile) {
        Path temp = JkUtilsPath.createTempDirectory("jeka-war");
        generateWarDir(temp);
        JkPathTree.of(temp).zipTo(destFile);
        JkPathTree.of(temp).deleteRoot();
    }

}
