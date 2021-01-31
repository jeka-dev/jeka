package dev.jeka.core.api.depmanagement.tooling;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.tooling.JkMavenMetadata.Versioning.JkSnapshot;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Object representation of the maven-metadata.xml file found in Maven
 * repositories for describing available timestamped snapshot available for a
 * given version. This is a not used for Maven2 repositories.
 * 
 * @author Jerome Angibaud
 */
public final class JkMavenMetadata {

    public static JkMavenMetadata of(JkVersionedModule versionedModule, String timestamp) {
        final JkMavenMetadata metadata = new JkMavenMetadata();
        metadata.groupId = versionedModule.getModuleId().getGroup();
        metadata.artifactId = versionedModule.getModuleId().getName();
        metadata.modelVersion = "1.1.0";
        metadata.version = versionedModule.getVersion().getValue();
        metadata.versioning = new Versioning();
        metadata.versioning.snapshot = new JkSnapshot(timestamp, 0);
        return metadata;
    }

    public static JkMavenMetadata of(JkModuleId moduleId) {
        final JkMavenMetadata metadata = new JkMavenMetadata();
        metadata.groupId = moduleId.getGroup();
        metadata.artifactId = moduleId.getName();
        metadata.modelVersion = "1.1.0";
        return metadata;
    }

    public static JkMavenMetadata of(InputStream inputStream) {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            final Element metadata = (org.w3c.dom.Element) doc.getElementsByTagName("metadata")
                    .item(0);
            final String modelVersion = metadata.getAttribute("modelVersion");
            final JkMavenMetadata result = new JkMavenMetadata();
            result.modelVersion = modelVersion;
            result.groupId = subValue(metadata, "groupId");
            result.artifactId = subValue(metadata, "artifactId");
            result.version = subValue(metadata, "version");
            final Element versioningEl = (Element) metadata.getElementsByTagName("versioning")
                    .item(0);
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

    private Versioning versioning = new Versioning();

    private JkMavenMetadata() {

    }

    public void updateSnapshot(String timestamp) {
        if (versioning == null) {
            this.versioning = new Versioning();
        }
        final int buildNumber = this.versioning.currentBuildNumber() + 1;
        this.versioning.snapshot = new JkSnapshot(timestamp, buildNumber);
        this.versioning.lastUpdate = timestamp.replace(".", "");
        this.versioning.snapshotVersions.clear();
    }

    public JkSnapshot currentSnapshot() {
        return this.versioning.snapshot;
    }

    void setFirstCurrentSnapshot(String timestamp) {
        this.versioning.snapshot = new Versioning.JkSnapshot(timestamp, 1);
    }

    public void addSnapshotVersion(String extension, String classifier) {
        final Versioning.SnapshotVersion snapshotVersion = new Versioning.SnapshotVersion();
        snapshotVersion.classifier = JkArtifactId.MAIN_ARTIFACT_NAME.equals(classifier) ? null : classifier;
        snapshotVersion.extension = extension;
        snapshotVersion.updated = this.versioning.lastUpdate;
        final String version = this.version.replace("-SNAPSHOT", "");
        final JkSnapshot snapshot = this.versioning.snapshot;
        snapshotVersion.value = version + "-" + snapshot.timestamp + "-" + snapshot.buildNumber;
        this.versioning.snapshotVersions.add(snapshotVersion);
    }

    public int currentBuildNumber() {
        return this.versioning.currentBuildNumber();
    }

    /*
     * see https://support.sonatype.com/entries/23778606-Why-are-the-latest-and-
     * release-tags-in-maven-metadata-xml-not-being-updated-after-deploying-
     * artifact
     */
    public void addVersion(String version, String timestamp) {
        if (!versioning.versions.contains(version)) {
            this.versioning.versions.add(version);
            Collections.sort(this.versioning.versions);
            versioning.latest = version; // 'latest' field is intended only for
            // maven-plugins
            if (!version.endsWith("-SNAPSHOT")) {
                this.versioning.release = version;
            }
        }
        this.versioning.lastUpdate = timestamp;
    }

    public void output(OutputStream outputStream) {
        try {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(
                    outputStream, "UTF-8");
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
        if (version != null) {
            writeElement(writer, "version", version);
            ln(writer);
            indent(writer, 2);
        }
        this.versioning.write(writer);
        ln(writer);
        writer.writeEndElement();
    }

    public static class Versioning {

        private Versioning() {
        }

        private Versioning(Element element) {
            final NodeList snapNodeList = element.getElementsByTagName("snapshot");
            if (snapNodeList.getLength() > 0) {
                this.snapshot = new JkSnapshot((Element) snapNodeList.item(0));
            }
            this.snapshotVersions = new LinkedList<>();

            final NodeList versionsNodeList = element.getElementsByTagName("versions");
            if (versionsNodeList.getLength() > 0) {
                final Element versions = (Element) element.getElementsByTagName("versions").item(0);
                final NodeList nodeList = versions.getElementsByTagName("version");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    final Element versionEl = (Element) nodeList.item(i);
                    this.versions.add(versionEl.getTextContent());
                }
            }

            final NodeList snapshotVersionsNodeList = element
                    .getElementsByTagName("snapshotVersions");
            if (snapshotVersionsNodeList.getLength() > 0) {
                final Element snapshotVersions = (Element) element.getElementsByTagName(
                        "snapshotVersions").item(0);
                final NodeList nodeList = snapshotVersions.getElementsByTagName("snapshotVersion");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    final Element snapshotVersionEl = (Element) nodeList.item(i);
                    this.snapshotVersions.add(new SnapshotVersion(snapshotVersionEl));
                }
            }
            this.latest = subValue(element, "latest");
            this.release = subValue(element, "release");
            this.lastUpdate = subValue(element, "lastUpdate");
        }

        private JkSnapshot snapshot;

        private String latest;

        private String release;

        private List<SnapshotVersion> snapshotVersions = new LinkedList<>();

        private String lastUpdate;

        private final List<String> versions = new LinkedList<>();

        private int currentBuildNumber() {
            if (snapshot == null) {
                return 0;
            }
            return snapshot.buildNumber;
        }

        private void write(XMLStreamWriter writer) throws XMLStreamException {
            writer.writeStartElement("versioning");
            if (latest != null) {
                ln(writer);
                indent(writer, 4);
                writeElement(writer, "latest", latest);
            }
            if (snapshot != null) {
                ln(writer);
                indent(writer, 4);
                snapshot.write(writer);
            }
            if (release != null) {
                ln(writer);
                indent(writer, 4);
                writeElement(writer, "release", release);
            }
            ln(writer);
            indent(writer, 4);
            if (!versions.isEmpty()) {
                writer.writeStartElement("versions");
                for (final String version : this.versions) {
                    ln(writer);
                    indent(writer, 6);
                    writeElement(writer, "version", version);
                }
                ln(writer);
                indent(writer, 4);
                writer.writeEndElement();

            }
            if (!snapshotVersions.isEmpty()) {
                writer.writeStartElement("snapshotVersions");
                for (final SnapshotVersion snapshotVersion : this.snapshotVersions) {
                    ln(writer);
                    indent(writer, 6);
                    snapshotVersion.write(writer);
                }
                ln(writer);
                indent(writer, 4);
                writer.writeEndElement();
            }
            ln(writer);
            indent(writer, 4);
            writeElement(writer, "lastUpdate", lastUpdate);
            ln(writer);
            indent(writer, 2);
            writer.writeEndElement();
        }

        public static class JkSnapshot {
            public final String timestamp;
            public final int buildNumber;

            JkSnapshot(String timestamp, int buildNumber) {
                this.buildNumber = buildNumber;
                this.timestamp = timestamp;
            }

            JkSnapshot(Element element) {
                this.buildNumber = Integer.parseInt(subValue(element, "buildNumber"));
                this.timestamp = subValue(element, "timestamp");
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
                this.extension = subValue(element, "getExtension");
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
                writeElement(writer, "getExtension", extension);
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
        final NodeList nodeList = el.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                if (element.getNodeName().equals(subTag)) {
                    return element.getTextContent();
                }
            }
        }
        return null;
    }

    private static void writeElement(XMLStreamWriter writer, String name, String value)
            throws XMLStreamException {
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

    public String lastUpdateTimestamp() {
        return this.versioning.lastUpdate;
    }

}
