package org.jerkar.api.depmanagement;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Object representation of the maven-metadata.xml file found in Maven repositories.
 * 
 * @author Jerome Angibaud
 */
class MavenMetadata {

	static MavenMetadata of(InputStream inputStream) {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			final Element metadata = (org.w3c.dom.Element) doc.getElementsByTagName("metadata").item(0);
			final String modelVersion = metadata.getAttribute("modelVersion");
			final Element groupId = (Element) metadata.getElementsByTagName("groupId").item(0);
			final Element artifactIf = (Element) metadata.getElementsByTagName("artifactId").item(0);
			final Element versioning = (Element) metadata.getElementsByTagName("versioning").item(0);
			final MavenMetadata result = new MavenMetadata();
			result.modelVersion = modelVersion;
			result.groupId = groupId.getNodeValue();
			result.artifactId = artifactIf.getNodeValue();
			result.versioning = new Versioning(versioning);
			return result;
		} catch (final Exception e) {
			throw new RuntimeException("Error while parsing .classpath file : ", e);
		}
	}


	private String modelVersion;

	private String groupId;

	private String artifactId;

	private Versioning versioning;

	void output(OutputStream outputStream) {
		try {
			final XMLStreamWriter writer = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(outputStream, "UTF-8");
			print(writer);
		} catch (final Exception e) {
			throw JkUtilsThrowable.unchecked(e);
		}
	}

	private void print(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("metadata");
		writer.writeAttribute("modelVersion", modelVersion);
		writeElement(writer, "groupId", groupId);
		writeElement(writer, "artifactId", artifactId);
		this.versioning.print(writer);
		writer.writeEndElement();
	}


	private static class Versioning {

		Versioning(Element element) {
			this.latest = subValue(element, "latest");
			this.release = subValue(element, "release");
			this.versions = new LinkedList<String>();
			final Element versions = (Element) element.getElementsByTagName("versions").item(0);
			final NodeList nodeList = versions.getElementsByTagName("version");
			for (int i=0; i < nodeList.getLength(); i++) {
				final Element version = (Element) nodeList.item(i);
				this.versions.add(version.getNodeValue());
			}
			this.lastUpdate = subValue(element, "lastUpdate");
		}

		private final String latest;

		private final String release;

		private final List<String> versions;

		private final String lastUpdate;

		private void print(XMLStreamWriter writer) throws XMLStreamException {
			writer.writeStartElement("versioning");
			writeElement(writer, "latest", latest);
			if (JkUtilsString.isBlank(release)) {
				writer.writeEmptyElement("release");
			} else {
				writeElement(writer, "release", release);
			}
			writer.writeStartElement("version");
			for (final String version : versions) {
				writeElement(writer, "version", version);
			}
			writer.writeEndElement();
			writeElement(writer, "lastUpdate", lastUpdate);
			writer.writeEndElement();
		}

	}

	private static String subValue(Element el, String subTag) {
		final Element element = (Element) el.getElementsByTagName(subTag).item(0);
		return element.getNodeValue();
	}

	private static void writeElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
		writer.writeStartElement("groupId");
		writer.writeCharacters(value);
		writer.writeEndElement();
	}

}
