package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.JkPomMetadata.JkDeveloperInfo;
import dev.jeka.core.api.depmanagement.JkPomMetadata.JkLicenseInfo;
import dev.jeka.core.api.depmanagement.JkPomMetadata.JkProjectInfo;
import dev.jeka.core.api.depmanagement.JkPomMetadata.JkScmInfo;
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
        final JkProjectInfo projectInfo = publicationInfo.getProjectInfo();
        if (projectInfo != null) {
            writeElement("  ", writer, "name", projectInfo.getName());
            writeElement("  ", writer, "description", projectInfo.getDescription());
            writeElement("  ", writer, "url", projectInfo.getUrl());
        }
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
        final JkScmInfo scm = publicationInfo.getScm();
        if (scm != null) {
            writer.writeCharacters("\n\n  ");
            writer.writeStartElement("scm");
            writer.writeCharacters("\n");
            writeElement("    ", writer, "connection", scm.getConnection());
            writeElement("    ", writer, "developerConnection", scm.getDeveloperConnection());
            writeElement("    ", writer, "url", scm.getUrl());
            writer.writeCharacters("  ");
            writer.writeEndElement();
        }

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
