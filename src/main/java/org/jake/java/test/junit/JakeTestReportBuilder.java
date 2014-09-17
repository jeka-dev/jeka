package org.jake.java.test.junit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jake.java.test.junit.JakeTestSuiteResult.Failure;

class JakeTestReportBuilder {

	private static final XMLOutputFactory factory = XMLOutputFactory
			.newInstance();

	private final JakeTestSuiteResult result;

	private JakeTestReportBuilder(JakeTestSuiteResult result) {
		this.result = result;
	}

	public static JakeTestReportBuilder of(JakeTestSuiteResult result) {
		return new JakeTestReportBuilder(result);
	}

	public void writeToFileSystem(File folder) {
		folder.mkdirs();
		final File file = new File(folder, "TEST-" + result.suiteName()
				+ ".xml");
		try {
			file.createNewFile();
			writeFile(file);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writeFile(File xmlFile) throws XMLStreamException, IOException {
		final XMLStreamWriter writer = factory
				.createXMLStreamWriter(new FileWriter(xmlFile));
		writer.writeStartDocument();
		writer.writeCharacters("\n");
		writer.writeStartElement("testsuite");
		writer.writeAttribute("skipped", Integer.toString(result.ignoreCount()));
		writer.writeAttribute("tests", Integer.toString(result.runCount()));
		writer.writeAttribute("failures",
				Integer.toString(result.assertErrorCount()));
		writer.writeAttribute("errors", Integer.toString(result.errorCount()));
		writer.writeAttribute("name", result.suiteName());
		writer.writeAttribute("time",
				Float.toString((float) result.durationInMillis() / 1000));
		writer.writeCharacters("\n");

		writeProperties(writer);
		writeTestCases(writer);

		writer.writeEndElement(); // ends 'testsuite'
		writer.writeEndDocument();

		writer.flush();
		writer.close();
	}

	private void writeProperties(XMLStreamWriter writer)
			throws XMLStreamException {
		writer.writeCharacters("  ");
		writer.writeStartElement("properties");
		for (final Object name : this.result.getSystemProperties().keySet()) {
			writer.writeCharacters("\n    ");
			writer.writeEmptyElement("property");
			writer.writeAttribute("value", System.getProperty(name.toString()));
			writer.writeAttribute("name", name.toString());
		}
		writer.writeCharacters("\n  ");
		writer.writeEndElement();
	}

	private void writeTestCases(XMLStreamWriter writer)
			throws XMLStreamException {
		for (final JakeTestSuiteResult.TestCaseResult testCaseResult : this.result
				.testCaseResults()) {
			writer.writeCharacters("\n  ");
			writer.writeStartElement("testcase");
			writer.writeAttribute("classname", testCaseResult.getClassName());
			writer.writeAttribute("name", testCaseResult.getTestName());
			if (testCaseResult.getDurationInSecond() != -1) {
				writer.writeAttribute("time", Float.toString(testCaseResult.getDurationInSecond()));
			}
			if (testCaseResult instanceof Failure) {
				final Failure failure = (Failure) testCaseResult;
				final String errorFailure = failure
						.getExceptionDescription().isAssertError() ? "failure"
								: "error";
				writer.writeCharacters("\n    ");
				writer.writeStartElement(errorFailure);
				writer.writeAttribute("message", failure
						.getExceptionDescription().getMessage());
				writer.writeAttribute("type", failure
						.getExceptionDescription().getClassName());
				final StringBuilder stringBuilder = new StringBuilder();
				for (final String line : failure
						.getExceptionDescription().stackTracesAsStrings()) {
					stringBuilder.append(line).append("\n");
				}
				stringBuilder.append("      ");
				writer.writeCData(stringBuilder.toString());
				writer.writeCharacters("\n    ");
				writer.writeEndElement();
			}
			writer.writeCharacters("\n  ");
			writer.writeEndElement();
		}
	}

}
