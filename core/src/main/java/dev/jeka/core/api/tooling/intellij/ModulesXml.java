/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ModulesXml {

    private static final String MODULES_XML_PATH = ".idea/modules.xml";

    private final Path projectBaseDir;

    private final List<Module> modules;

    ModulesXml(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir.toAbsolutePath().normalize();
        this.modules = filePaths(projectBaseDir).stream()
                .map(Module::new)
                .collect(Collectors.toList());
    }

    static ModulesXml find(Path moduleBaseDir) {
        moduleBaseDir = moduleBaseDir.toAbsolutePath();
        if (Files.exists(moduleBaseDir.resolve(MODULES_XML_PATH))) {
            return new ModulesXml(moduleBaseDir);
        }
        if (moduleBaseDir.getRoot().equals(moduleBaseDir.getParent())) {
            JkLog.warn("No modules.xml file in filesystem hierarchy starting from  " + moduleBaseDir);
            return null;
        }
        return find(moduleBaseDir.getParent());

    }

    String findModuleName(Path modulePath) {
        return modules.stream()
                .filter(module -> modulePath.toAbsolutePath().normalize().equals(module.getModulePath()))
                .map(Module::getModuleName)
                .findFirst().orElse(null);
    }

    private class Module {

        private final String filePath;

        private Module(String filePath) {
            this.filePath = filePath;
        }

        Path getModulePath() {
            String resolvedImlPath = filePath.replace("$PROJECT_DIR$", projectBaseDir.toString());
            Path imlPath = Paths.get(resolvedImlPath);
            Path candidate = imlPath.getParent();
            if (".idea".equals(candidate.getFileName().toString())) {
                candidate = candidate.getParent();
            }
            return candidate;
        }

        String getModuleName() {
            return JkUtilsString.substringBeforeLast(Paths.get(filePath).getFileName().toString(), ".iml");
        }
    }

    private static List<String> filePaths(Path projectBaseDir) {
        Path modulesXmlPath = projectBaseDir.resolve(MODULES_XML_PATH);
        if (!Files.exists(modulesXmlPath)) {
            JkLog.warn("Modules xml file does not exist: " + modulesXmlPath);
            return Collections.emptyList();
        }
        JkDomDocument doc = JkDomDocument.parse(modulesXmlPath);
        return doc.root().get("component").get("modules").children("module").stream()
                .map(el -> el.attr("filepath"))
                .collect(Collectors.toList());
    }

}
