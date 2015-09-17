package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.JkMavenPublicationInfo.JkDeveloperInfo;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo.JkLicenseInfo;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo.JkProjectInfo;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo.JkScmInfo;
import org.jerkar.api.system.JkInfo;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;

final class PomTemplateGenerator {

	private static final String TOKEN = "____jerkar.maven.extraInfo____";

	private static final String VERSION_TOKEN = "____jerkarVersion____";

	public static File generateTemplate(JkMavenPublicationInfo publicationInfo) {
		final String firstTemplate = JkUtilsIO.read(PomTemplateGenerator.class.getResource("pom-full.template"));
		String extraXml;
		try {
			extraXml = extraXml(publicationInfo);
		} catch (final XMLStreamException e) {
			throw new RuntimeException(e);
		}
		String completeTemplate = firstTemplate.replace(TOKEN, extraXml);
		completeTemplate = completeTemplate.replace(VERSION_TOKEN, JkInfo.jerkarVersion());
		final File result = JkUtilsFile.tempFile("jerkar-pom", ".template");
		JkUtilsFile.writeString(result, completeTemplate, false);
		return result;
	}

	private static String extraXml(JkMavenPublicationInfo publicationInfo) throws XMLStreamException, FactoryConfigurationError {
		final StringWriter stringWriter = new StringWriter();
		final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
		final JkProjectInfo projectInfo = publicationInfo.project;
		if (projectInfo != null) {
			writeElement("  ", writer, "name", projectInfo.name);
			writeElement("  ", writer, "description", projectInfo.description);
			writeElement("  ", writer, "url", projectInfo.url);
		}
		final List<JkLicenseInfo> licenses = publicationInfo.licenses;
		if (!licenses.isEmpty()) {
			writer.writeCharacters("\n");
			writer.writeCharacters("  ");
			writer.writeStartElement("licenses");
			for (final JkLicenseInfo license : licenses) {
				writer.writeCharacters("\n    ");
				writer.writeStartElement("license");
				writer.writeCharacters("\n");
				writeElement("      ", writer, "name", license.name);
				writeElement("      ", writer, "url", license.url);
				writer.writeCharacters("    ");
				writer.writeEndElement();
			}
			writer.writeCharacters("\n  ");
			writer.writeEndElement();
		}
		final List<JkDeveloperInfo> developers = publicationInfo.devs;
		if (!developers.isEmpty()) {
			writer.writeCharacters("\n\n");
			writer.writeCharacters("  ");
			writer.writeStartElement("developers");
			for (final JkDeveloperInfo developer : developers) {
				writer.writeCharacters("\n    ");
				writer.writeStartElement("developer");
				writer.writeCharacters("\n");
				writeElement("      ", writer, "name", developer.name);
				writeElement("      ", writer, "email", developer.email);
				writeElement("      ", writer, "organization", developer.organisation);
				writeElement("      ", writer, "organizationUrl", developer.organisationUrl);
				writer.writeCharacters("    ");
				writer.writeEndElement();
			}
			writer.writeCharacters("\n  ");
			writer.writeEndElement();
		}
		final JkScmInfo scm = publicationInfo.scm;
		if (scm != null) {
			writer.writeCharacters("\n\n  ");
			writer.writeStartElement("scm");
			writer.writeCharacters("\n");
			writeElement("    ", writer, "connection", scm.connection);
			writeElement("    ", writer, "developerConnection", scm.developerConnection);
			writeElement("    ", writer, "url", scm.url);
			writer.writeCharacters("  ");
			writer.writeEndElement();
		}

		writer.flush();
		writer.close();
		return stringWriter.toString();
	}

	private static void writeElement(XMLStreamWriter writer, String elementName, String text) throws XMLStreamException {
		if (text == null) {
			return;
		}
		writer.writeStartElement(elementName);
		writer.writeCharacters(text);
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	private static void writeElement(String ident, XMLStreamWriter writer, String elementName, String text) throws XMLStreamException {
		if (text == null) {
			return;
		}
		writer.writeCharacters(ident);
		writeElement(writer, elementName, text);
	}

}
