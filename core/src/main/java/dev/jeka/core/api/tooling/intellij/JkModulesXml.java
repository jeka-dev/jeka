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
import dev.jeka.core.api.marshalling.xml.JkDomElement;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import org.w3c.dom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JkModulesXml {

    private static final String MODULES_XML_PATH = ".idea/modules.xml";

    private final Path projectBaseDir;

    private final Path modulesXmlFile;

    private JkModulesXml(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir.toAbsolutePath().normalize();
        this.modulesXmlFile = projectBaseDir.resolve(MODULES_XML_PATH);
    }

    public static JkModulesXml of(Path baseDir) {
        return new JkModulesXml(baseDir);
    }

    public JkModulesXml createIfAbsentOrInvalid() {
        if (!Files.exists(modulesXmlFile)) {
            createEmpty();
        } else {
            try {
                JkDomDocument.parse(modulesXmlFile);
            } catch (RuntimeException e) {
                JkLog.warn(".idea/module.xml not readable. Recreate it from scratch.");
                createEmpty();
            }
        }
        return this;
    }

    static JkModulesXml find(Path moduleBaseDir) {
        moduleBaseDir = moduleBaseDir.toAbsolutePath();
        if (Files.exists(moduleBaseDir.resolve(MODULES_XML_PATH))) {
            return of(moduleBaseDir);
        }
        if (Files.exists(moduleBaseDir.resolve(".idea/workspace.xml"))) {
            JkLog.warn("No modules.xml file in filesystem hierarchy.");
            return null;
        }
        if (moduleBaseDir.getRoot().equals(moduleBaseDir.getParent())) {
            JkLog.warn("No modules.xml file in filesystem hierarchy starting from  " + moduleBaseDir);
            return null;
        }
        return find(moduleBaseDir.getParent());
    }

    public void addImlIfNeeded(Path imlFile) {
        String moduleRelPath = imlFile.toString();
        if (imlFile.isAbsolute()) {
            moduleRelPath = projectBaseDir.relativize(imlFile).toString();
        }
        String relPath = moduleRelPath;
        boolean present = getModules().stream()
                .anyMatch(module -> module.filePath.equals("$PROJECT_DIR$/" + relPath));
        if (!present) {
            Path moduleXmlPath = projectBaseDir.resolve(MODULES_XML_PATH);
            JkUtilsPath.createFileSafely(moduleXmlPath);
            JkDomDocument doc = JkDomDocument.parse(projectBaseDir.resolve(MODULES_XML_PATH));
            JkDomElement modulesEl = doc.root().get("component").get("modules");
            Element element = doc.getW3cDocument().createElement("module");
            element.setAttribute("filepath", "$PROJECT_DIR$/" + moduleRelPath);
            element.setAttribute("fileurl", "file://$PROJECT_DIR$/" + moduleRelPath);

            modulesEl.getW3cElement().appendChild(element);
            doc.save(moduleXmlPath);
        }
    }

    public void removeIfNeeded(Path imlFile) {
        String moduleRelPath = imlFile.toString();
        if (imlFile.isAbsolute()) {
            moduleRelPath = projectBaseDir.relativize(imlFile).toString();
        }
        String relPath = moduleRelPath;
        boolean present = getModules().stream()
                .anyMatch(module -> module.filePath.equals("$PROJECT_DIR$/" + relPath));
        if (present) {
            Path moduleXmlPath = projectBaseDir.resolve(MODULES_XML_PATH);
            JkDomDocument doc = JkDomDocument.parse(projectBaseDir.resolve(MODULES_XML_PATH));
            doc.root().get("component").get("modules").children("module").stream()
                            .filter(child -> child.attr("filepath").equals("$PROJECT_DIR$/" + relPath))
                            .forEach(JkDomElement::remove);
            JkUtilsPath.deleteQuietly(moduleXmlPath, true);
            doc.save(moduleXmlPath);
        }
    }

    String findModuleName(Path modulePath) {
        return getModules().stream()
                .filter(module -> modulePath.toAbsolutePath().normalize().equals(module.getModulePath()))
                .map(Module::getModuleName)
                .findFirst().orElse(null);
    }

    private void createEmpty() {
        JkDomDocument domDocument = JkDomDocument.of("project");
        domDocument.root().make()//.attr("version", "4")
                .get("component").make()//.attr("name", "ProjectModuleManager")
                    .get("modules").make();
        domDocument.save(modulesXmlFile);
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

    private List<Module> getModules() {
        return filePaths(projectBaseDir).stream()
                .map(Module::new)
                .collect(Collectors.toList());
    }

    private static List<String> filePaths(Path projectBaseDir) {
        Path modulesXmlPath = projectBaseDir.resolve(MODULES_XML_PATH);
        if (!Files.exists(modulesXmlPath)) {
            JkLog.warn("Modules xml file does not exist: " + modulesXmlPath);
            return Collections.emptyList();
        }
        JkDomDocument doc;
        try {
            doc = JkDomDocument.parse(modulesXmlPath);
        }  catch (RuntimeException e) {
            JkLog.warn("Modules xml file is not valid, return empty.");
            return Collections.emptyList();
        }

        return doc.root().get("component").get("modules").children("module").stream()
                .map(el -> el.attr("filepath"))
                .collect(Collectors.toList());
    }

}
