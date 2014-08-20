package org.jake.java.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class JakeTestReportBuilder {

	private static final XMLOutputFactory factory = XMLOutputFactory.newInstance();

	private final JakeTestSuiteResult result;

	private JakeTestReportBuilder(JakeTestSuiteResult result) {
		this.result = result;
	}

	public static JakeTestReportBuilder of(JakeTestSuiteResult result) {
		return new JakeTestReportBuilder(result);
	}

	public void writeToFileSystem(File folder) {
		final File file = new File(folder, "TEST-all.xml");
		try {
			writeFile(file);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
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

		writeProperties(writer);
		writeFailures(writer);

		writer.writeEndDocument();

		writer.flush();
		writer.close();
	}

	private void writeProperties(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("properties");
		for (final Object name : System.getProperties().keySet()) {
			writer.writeStartElement("property");
			writer.writeAttribute("value", System.getProperty(name.toString()));
			writer.writeAttribute("name", name.toString());
		}
		writer.writeEndElement();
	}

	private void writeFailures(XMLStreamWriter writer) throws XMLStreamException {
		for (final JakeTestSuiteResult.Failure failure : this.result.failures()) {
			writer.writeStartElement("testcase");
			writer.writeAttribute("classname", failure.getClassName());
			writer.writeAttribute("name", failure.getTestName());
			writer.writeEndElement();
			writer.writeStartElement("failure");
			writer.writeAttribute("message", failure.getExceptionDescription().getMessage());
			for (final String line : failure.getExceptionDescription().stackTracesAsStrings()) {
				writer.writeCData(line + "\n");
			}
			writer.writeEndElement();
		}



	}


}
