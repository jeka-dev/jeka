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

package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.publication.JkPomMetadata.JkDeveloperInfo;
import dev.jeka.core.api.depmanagement.publication.JkPomMetadata.JkLicenseInfo;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsPath;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

public final class JkPomTemplateGenerator {

    private static final String TOKEN = "____jeka.maven.extraInfo____";

    private static final String VERSION_TOKEN = "____jekaVersion____";

    public static Path generateTemplate(JkPomMetadata publicationInfo) {
        final String firstTemplate = JkUtilsIO.read(JkPomTemplateGenerator.class
                .getResource("pom-full.template"));
        String extraXml;
        try {
            extraXml = extraXml(publicationInfo);
        } catch (final XMLStreamException e) {
            throw new RuntimeException(e);
        }
        String completeTemplate = firstTemplate.replace(TOKEN, extraXml);
        final String JekaVersion = JkUtilsObject.firstNonNull(JkInfo.getJekaVersion(), "Development version");
        completeTemplate = completeTemplate.replace(VERSION_TOKEN, JekaVersion);
        final Path result = JkUtilsPath.createTempFile("jeka-pom", ".template");
        JkUtilsPath.write(result, completeTemplate.getBytes());
        return result;
    }

    private static String extraXml(JkPomMetadata publicationInfo)
            throws XMLStreamException, FactoryConfigurationError {
        final StringWriter stringWriter = new StringWriter();
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(
                stringWriter);
        writeElement("  ", writer, "name", publicationInfo.getProjectName());
        writeElement("  ", writer, "description", publicationInfo.getProjectDescription());
        writeElement("  ", writer, "url", publicationInfo.getProjectUrl());
        final List<JkLicenseInfo> licenses = publicationInfo.getLicenses();
        if (!licenses.isEmpty()) {
            writer.writeCharacters("\n");
            writer.writeCharacters("  ");
            writer.writeStartElement("licenses");
            for (final JkLicenseInfo license : licenses) {
                writer.writeCharacters("\n    ");
                writer.writeStartElement("license");
                writer.writeCharacters("\n");
                writeElement("      ", writer, "name", license.getName());
                writeElement("      ", writer, "url", license.getUrl());
                writer.writeCharacters("    ");
                writer.writeEndElement();
            }
            writer.writeCharacters("\n  ");
            writer.writeEndElement();
        }
        final List<JkDeveloperInfo> developers = publicationInfo.getDevelopers();
        if (!developers.isEmpty()) {
            writer.writeCharacters("\n\n");
            writer.writeCharacters("  ");
            writer.writeStartElement("developers");
            for (final JkDeveloperInfo developer : developers) {
                writer.writeCharacters("\n    ");
                writer.writeStartElement("developer");
                writer.writeCharacters("\n");
                writeElement("      ", writer, "name", developer.getName());
                writeElement("      ", writer, "email", developer.getEmail());
                writeElement("      ", writer, "organization", developer.getOrganisation());
                writeElement("      ", writer, "organizationUrl", developer.getOrganisationUrl());
                writer.writeCharacters("    ");
                writer.writeEndElement();
            }
            writer.writeCharacters("\n  ");
            writer.writeEndElement();
        }
        writer.writeCharacters("\n\n  ");
        writer.writeStartElement("scm");
        writer.writeCharacters("\n");
        writeElement("    ", writer, "connection", publicationInfo.getScmConnection());
        writeElement("    ", writer, "developerConnection", publicationInfo.getScmDeveloperConnection());
        writeElement("    ", writer, "url", publicationInfo.getScmUrl());
        writer.writeCharacters("  ");
        writer.writeEndElement();

        writer.flush();
        writer.close();
        return stringWriter.toString();
    }

    private static void writeElement(XMLStreamWriter writer, String elementName, String text)
            throws XMLStreamException {
        if (text == null) {
            return;
        }
        writer.writeStartElement(elementName);
        writer.writeCharacters(text);
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private static void writeElement(String indent, XMLStreamWriter writer, String elementName,
            String text) throws XMLStreamException {
        if (text == null) {
            return;
        }
        writer.writeCharacters(indent);
        writeElement(writer, elementName, text);
    }

}
