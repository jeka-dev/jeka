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

package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class IntelliJProject {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

    private final Path rootDir;

    private IntelliJProject(Path rootDir) {
        this.rootDir = rootDir;
    }

    public static IntelliJProject of(Path rootDir) {
        rootDir = rootDir.toAbsolutePath();
        return new IntelliJProject(rootDir);
    }

    public IntelliJProject deleteWorkspaceXml() {
        JkUtilsPath.deleteIfExists(rootDir.resolve(".idea/workspace.xml"));
        return this;
    }

    public Path getModulesXmlPath() {
        return rootDir.resolve(".idea/modules.xml");
    }

    /**
     * Regenerates the modules.xml file based on the .iml files found in the project  root directory and its subdirectories.
     */
    public void regenerateModulesXml() {
        List<Path> imlFiles = findImlFiles();
        generateModulesXml(imlFiles);
    }

    /**
     * Finds all .iml files in the specified root directory and its subdirectories.
     */
    public List<Path> findImlFiles() {
        return JkPathTree.of(rootDir).andMatching(true, "**.iml")
                .andMatching(false, ".jeka-work/**/*")
                .andMatching(false, ".idea/output/**/*")
                .getFiles();
    }

    /**
     * Generates an XML file containing module information based on the provided .iml file paths.
     *
     * @param imlPaths A collection of .iml file paths, or a single path.
     */
    public IntelliJProject generateModulesXml(Iterable<Path> imlPaths) {
        try {
            JkLog.startTask("generate-modules.xml");
            List<Path> imlFiles =JkUtilsPath.disambiguate(imlPaths);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos, ENCODING);
            writer.writeStartDocument(ENCODING, "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("project");
            writer.writeCharacters("\n" + T1);
            writer.writeStartElement("component");
            writer.writeAttribute("name", "ProjectModuleManager");
            writer.writeCharacters("\n" + T2);
            writer.writeStartElement("modules");
            writer.writeCharacters("\n");
            for (final Path iml : imlFiles) {
                final Path relPath = rootDir.toAbsolutePath().normalize()
                        .relativize(iml.toAbsolutePath().normalize());
                JkLog.info("Iml file detected : " + relPath);
                final String path = "$PROJECT_DIR$/" + relPath.toString().replace('\\', '/');
                writer.writeCharacters(T3);
                writer.writeEmptyElement("module");
                writer.writeAttribute("fileurl", "file://" + path);
                writer.writeAttribute("filepath", path);
                writer.writeCharacters("\n");
            }
            writer.writeCharacters(T2);
            writer.writeEndElement();
            writer.writeCharacters("\n" + T1);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            Path outputFile = getModulesXmlPath();
            JkUtilsPath.deleteIfExists(outputFile);
            JkUtilsPath.createDirectories(outputFile.getParent());
            Files.write(outputFile, baos.toByteArray());
            JkLog.info("File generated at : " + outputFile);
            return this;
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        } finally {
            JkLog.endTask();
        }
    }

}
