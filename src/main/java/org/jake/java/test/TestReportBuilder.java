package org.jake.java.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class TestReportBuilder {

	private static final XMLOutputFactory factory = XMLOutputFactory.newInstance();

	private JakeTestResult result;

	public void writeToFileSystem(File folder) {

	}

	private void writeFile(File xmlFile) throws XMLStreamException, IOException {
		final XMLStreamWriter writer = factory.createXMLStreamWriter(new FileWriter(xmlFile));

		writer.writeStartDocument();
		writer.writeStartElement("testsuite");
		writer.writeAttribute("skipped", Integer.toString(result.ignoreCount()));
		writer.writeAttribute("tests", Integer.toString(result.runCount()));
		writer.writeAttribute("failures", Integer.toString(result.failureCount()));
		writer.writeAttribute("errors", "to be fixed");
		writer.writeAttribute("name", "to be fixed");
		writer.writeAttribute("time",  Float.toString((float)result.durationInMillis()/1000));


		writer.writeEndElement();
		writer.writeEndDocument();

		writer.flush();
		writer.close();
	}

	private void writeProperties(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("properties");
		for (final Object name : System.getProperties().keySet()) {
			writer.writeStartElement("property");
			writer.writeAttribute(name, System.get));
		}
	}


}
