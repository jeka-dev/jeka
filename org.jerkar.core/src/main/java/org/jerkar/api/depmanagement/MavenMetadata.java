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
import org.jerkar.api.utils.JkUtilsTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Object representation of the maven-metadata.xml file found in Maven repositories.
 * 
 * @author Jerome Angibaud
 */
class MavenMetadata {

	static MavenMetadata of(JkVersionedModule versionedModule) {
		final MavenMetadata metadata = new MavenMetadata();
		metadata.groupId = versionedModule.moduleId().group();
		metadata.artifactId = versionedModule.moduleId().name();
		metadata.modelVersion = "1.1.0";
		metadata.version = versionedModule.version().name();
		return metadata;
	}

	static MavenMetadata of(InputStream inputStream) {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			final Element metadata = (org.w3c.dom.Element) doc.getElementsByTagName("metadata").item(0);
			final String modelVersion = metadata.getAttribute("modelVersion");
			final MavenMetadata result = new MavenMetadata();
			result.modelVersion = modelVersion;
			result.groupId = subValue(metadata, "groupId");
			result.artifactId = subValue(metadata, "artifactId");
			result.version = subValue(metadata, "version");
			final Element versioningEl = (Element) metadata.getElementsByTagName("versioning").item(0);
			result.versioning = new Versioning(versioningEl);
			return result;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String modelVersion;

	private String groupId;

	private String artifactId;

	private String version;

	private Versioning versioning;

	private MavenMetadata() {

	}

	void updateSnapshot() {
		if (versioning == null) {
			this.versioning = new Versioning();
		}
		final int buildNumber = this.versioning.currentBuildNumber() + 1;
		final String ts = JkUtilsTime.nowUtc("yyyyMMdd.HHmmss");
		final org.jerkar.api.depmanagement.MavenMetadata.Versioning.Snapshot snapshot
		= new org.jerkar.api.depmanagement.MavenMetadata.Versioning.Snapshot(ts, buildNumber);
		this.versioning.snapshot = snapshot;
		this.versioning.lastUpdate = ts.replace(".", "");
		this.versioning.snapshotVersions.clear();
	}

	public void addSnapshotVersion(String extension, String classifier) {
		final org.jerkar.api.depmanagement.MavenMetadata.Versioning.SnapshotVersion snapshotVersion
		= new org.jerkar.api.depmanagement.MavenMetadata.Versioning.SnapshotVersion();
		snapshotVersion.classifier = classifier;
		snapshotVersion.extension = extension;
		snapshotVersion.updated = this.versioning.lastUpdate;
		final String version = this.version.replace("-SNAPSHOT", "");
		snapshotVersion.value = version + "-" + this.versioning.snapshot.timestamp + "-" + this.versioning.snapshot.buildNumber;
		this.versioning.snapshotVersions.add(snapshotVersion);
	}

	void output(OutputStream outputStream) {
		try {
			final XMLStreamWriter writer = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(outputStream, "UTF-8");
			write(writer);
		} catch (final Exception e) {
			throw JkUtilsThrowable.unchecked(e);
		}
	}

	private void write(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("metadata");
		writer.writeAttribute("modelVersion", modelVersion);
		ln(writer);
		indent(writer, 2);
		writeElement(writer, "groupId", groupId);
		ln(writer);
		indent(writer, 2);
		writeElement(writer, "artifactId", artifactId);
		ln(writer);
		indent(writer, 2);
		writeElement(writer, "version", version);
		ln(writer);
		indent(writer, 2);
		this.versioning.write(writer);
		ln(writer);
		writer.writeEndElement();
	}


	private static class Versioning {

		private Versioning() {
		}

		Versioning(Element element) {
			this.snapshot = new Snapshot((Element) element.getElementsByTagName("snapshot").item(0));
			this.snapshotVersions = new LinkedList<SnapshotVersion>();
			final Element snapshotVersions = (Element) element.getElementsByTagName("snapshotVersions").item(0);
			final NodeList nodeList = snapshotVersions.getElementsByTagName("snapshotVersion");
			for (int i=0; i < nodeList.getLength(); i++) {
				final Element snapshotVersionEl = (Element) nodeList.item(i);
				this.snapshotVersions.add(new SnapshotVersion(snapshotVersionEl));
			}
			this.lastUpdate = subValue(element, "lastUpdate");
		}

		private Snapshot snapshot;

		private List<SnapshotVersion> snapshotVersions = new LinkedList<MavenMetadata.Versioning.SnapshotVersion>();

		private String lastUpdate;

		private int currentBuildNumber() {
			if (snapshot == null) {
				return 0;
			}
			return snapshot.buildNumber;
		}

		private void write(XMLStreamWriter writer) throws XMLStreamException {
			writer.writeStartElement("versioning");
			ln(writer);
			indent(writer, 4);
			snapshot.write(writer);
			ln(writer);
			indent(writer, 4);
			writeElement(writer, "lastUpdate", lastUpdate);
			ln(writer);
			indent(writer, 4);
			writer.writeStartElement("snapshotVersions");
			for (final SnapshotVersion snapshotVersion : this.snapshotVersions) {
				ln(writer);
				indent(writer, 6);
				snapshotVersion.write(writer);
			}
			ln(writer);
			indent(writer, 4);
			writer.writeEndElement();
			ln(writer);
			indent(writer, 2);
			writer.writeEndElement();
		}

		private static class Snapshot {
			private final String timestamp;
			private final int buildNumber;
			private final List<SnapshotVersion> snapshotVersions;

			Snapshot(String timestamp, int buildNumber) {
				this.buildNumber = buildNumber;
				this.timestamp = timestamp;
				this.snapshotVersions = new LinkedList<MavenMetadata.Versioning.SnapshotVersion>();
			}

			Snapshot(Element element) {
				this.buildNumber = Integer.parseInt(subValue(element, "buildNumber"));
				this.timestamp = subValue(element, "timestamp");
				this.snapshotVersions = new LinkedList<MavenMetadata.Versioning.SnapshotVersion>();
			}

			void write(XMLStreamWriter writer) throws XMLStreamException {
				writer.writeStartElement("snapshot");
				ln(writer);
				indent(writer, 6);
				writeElement(writer, "timestamp", timestamp);
				ln(writer);
				indent(writer, 6);
				writeElement(writer, "buildNumber", Integer.toString(buildNumber));
				ln(writer);
				indent(writer, 4);
				writer.writeEndElement();
			}

		}

		private static class SnapshotVersion {
			String classifier;
			String extension;
			String value;
			String updated;

			private SnapshotVersion() {

			}

			SnapshotVersion(Element element) {
				this.classifier = subValue(element, "classifier");
				this.extension = subValue(element, "extension");
				this.updated = subValue(element, "updated");
				this.value = subValue(element, "value");
			}

			void write(XMLStreamWriter writer) throws XMLStreamException {
				writer.writeStartElement("snapshotVersion");
				if (classifier != null) {
					ln(writer);
					indent(writer, 8);
					writeElement(writer, "classifier", classifier);
				}
				ln(writer);
				indent(writer, 8);
				writeElement(writer, "extension", extension);
				ln(writer);
				indent(writer, 8);
				writeElement(writer, "updated", updated);
				ln(writer);
				indent(writer, 8);
				writeElement(writer, "value", value);
				ln(writer);
				indent(writer, 6);
				writer.writeEndElement();
			}

		}

	}

	private static String subValue(Element el, String subTag) {
		final Element element = (Element) el.getElementsByTagName(subTag).item(0);
		return element.getTextContent();
	}

	private static void writeElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
		writer.writeStartElement(name);
		writer.writeCharacters(value);
		writer.writeEndElement();
	}

	private static void ln(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeCharacters("\n");
	}

	private static void indent(XMLStreamWriter writer, int deep) throws XMLStreamException {
		writer.writeCharacters(JkUtilsString.repeat(" ", deep));
	}

}
