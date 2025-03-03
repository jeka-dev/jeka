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

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for working with IntelliJ IDEA's `misc.xml` configuration file.
 * Provides methods to verify the existence of the file and set the JDK configuration
 * for a project.
 */
public class JkMiscXml {

    private final Path miscXmlPath;

    private JkMiscXml(Path miscXmlPath) {
        this.miscXmlPath = miscXmlPath;
    }

    public static JkMiscXml ofBaseDir(Path baseDir) {
        return new JkMiscXml(baseDir.resolve(".idea").resolve("misc.xml"));
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
        xmlDoc.root().get("component")
                .attr("project-jdk-name", jdkName)
                .attr("project-jdk-type", "JavaSDK");
        xmlDoc.save(miscXmlPath);
    }
}
