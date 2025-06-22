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

import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomElement;
import dev.jeka.core.api.marshalling.xml.JkDomXPath;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for working with IntelliJ IDEA's `misc.xml` configuration file.
 * Provides methods to verify the existence of the file and set the JDK configuration
 * for a project.
 */
public class JkMiscXml {

    private final Path miscXmlPath;

    private static final String PROJECT_ROOT_MANAGER= "ProjectRootManager";

    private JkMiscXml(Path miscXmlPath) {
        this.miscXmlPath = miscXmlPath;
    }

    public static JkMiscXml ofBaseDir(Path baseDir) {
        return new JkMiscXml(baseDir.resolve(".idea").resolve("misc.xml"));
    }

    public static JkMiscXml find(Path baseDir) {
        JkMiscXml candidate = JkMiscXml.ofBaseDir(baseDir);
        if (Files.exists(candidate.miscXmlPath)) {
            return candidate;
        }
        if (Files.exists(baseDir.resolve("workspace.xml"))) {
            JkLog.warn("No misc.xml found at " + baseDir);
            return null;
        }
        if (baseDir.getParent() == null) {
            return null;
        }
        return find(baseDir.getParent());
    }

    /**
     * Checks if the `misc.xml` file exists at the specified path.
     */
    public boolean exists() {
        return Files.exists(miscXmlPath);
    }

    /**
     * Sets the JDK configuration in the `misc.xml` file for an IntelliJ IDEA project.
     * Updates the `project-jdk-name` and `project-jdk-type` attributes within the document.
     */
    public void setJdk(String jdkName) {
        JkDomDocument xmlDoc = JkDomDocument.parse(miscXmlPath);
        xmlDoc.root().children("component").stream()
                .filter(el -> PROJECT_ROOT_MANAGER.equals(el.attr("name")))
                .findFirst()
                .ifPresent(el -> {
                    el.attr("project-jdk-name", jdkName);
                    el.attr("project-jdk-type", "JavaSDK");
                });
        xmlDoc.save(miscXmlPath);
    }

    public JkJavaVersion guessProjectJavaVersion() {
        JkDomDocument xmlDoc;
        try {
            xmlDoc = JkDomDocument.parse(miscXmlPath);
        } catch (RuntimeException e) {
            return null;
        }

        JkDomElement el = xmlDoc.root().xPath("/project/component[@name='ProjectRootManager']").stream()
                .findFirst().orElse(null);

        if (el == null) {
            return null;
        }
        String projectJdkName = el.attr("project-jdk-name");
        String languageLevel = el.attr("languageLevel");

        JkJavaVersion result = Utils.guessFromJProjectJdkName(projectJdkName);
        if (result == null) {
            result = guessFromJLevelLanguage(languageLevel);
        }
        return result;
    }

    private static JkJavaVersion guessFromJLevelLanguage(String languageLevel) {
        if (languageLevel == null) {
            return null;
        }
        String digits = JkUtilsString.substringAfterLast("JDK_", languageLevel);
        if ("1.8".equals(digits)) {
            return JkJavaVersion.V8;
        }
        try {
            int value = Integer.parseInt(digits);
            if (value > 8 ) {
                return JkJavaVersion.of(digits);
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }



}
