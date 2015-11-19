package org.jerkar.api.java.junit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.java.junit.JkTestSuiteResult.IgnoredCase;
import org.jerkar.api.java.junit.JkTestSuiteResult.TestCaseFailure;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;

final class TestReportBuilder {

    private static final String TEXT_HEAD = JkUtilsString.repeat("-", 79);

    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    private final JkTestSuiteResult result;

    private TestReportBuilder(JkTestSuiteResult result) {
        this.result = result;
    }

    public static TestReportBuilder of(JkTestSuiteResult result) {
        return new TestReportBuilder(result);
    }

    public void writeToFileSystem(File folder) {
        folder.mkdirs();
        final File xmlFile = new File(folder, "TEST-" + result.suiteName() + ".xml");
        final File textFile = new File(folder, result.suiteName() + ".txt");
        try {
            xmlFile.createNewFile();
            textFile.createNewFile();
            writeXmlFile(xmlFile);
            writeTxtFile(textFile);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeTxtFile(File txtFile) {
        final StringBuilder builder = new StringBuilder(TEXT_HEAD).append("\n")
                .append("Test set: ").append(result.suiteName()).append("\n").append(TEXT_HEAD)
                .append("\n").append("Tests run: ").append(result.runCount()).append(", ")
                .append("Failures: ").append(result.assertErrorCount()).append(", ")
                .append("Errors: ").append(result.errorCount()).append(", ").append("Skipped: ")
                .append(result.ignoreCount()).append(", ").append("Time elapsed: ")
                .append(result.durationInMillis() / 1000f).append(" sec");
        JkUtilsFile.writeString(txtFile, builder.toString(), false);
    }

    private void writeXmlFile(File xmlFile) throws XMLStreamException, IOException {
        final XMLStreamWriter writer = FACTORY.createXMLStreamWriter(new FileWriter(xmlFile));
        writer.writeStartDocument();
        writer.writeCharacters("\n");
        writer.writeStartElement("testsuite");
        writer.writeAttribute("skipped", Integer.toString(result.ignoreCount()));
        writer.writeAttribute("tests", Integer.toString(result.runCount()));
        writer.writeAttribute("failures", Integer.toString(result.assertErrorCount()));
        writer.writeAttribute("errors", Integer.toString(result.errorCount()));
        writer.writeAttribute("name", result.suiteName());
        writer.writeAttribute("time", Float.toString(result.durationInMillis() / 1000f));
        writer.writeCharacters("\n");

        writeProperties(writer);
        writeTestCases(writer);

        writer.writeEndElement(); // ends 'testsuite'
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    private void writeProperties(XMLStreamWriter writer) throws XMLStreamException {
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

    private void writeTestCases(XMLStreamWriter writer) throws XMLStreamException {
        for (final JkTestSuiteResult.TestCaseResult testCaseResult : this.result.testCaseResults()) {
            writer.writeCharacters("\n  ");
            writer.writeStartElement("testcase");
            writer.writeAttribute("classname", testCaseResult.getClassName());
            writer.writeAttribute("name", testCaseResult.getTestName());
            if (testCaseResult.getDurationInSecond() != -1) {
                writer.writeAttribute("time", Float.toString(testCaseResult.getDurationInSecond()));
            } else {
                writer.writeAttribute("time", "0.000");
            }
            if (testCaseResult instanceof TestCaseFailure) {
                final TestCaseFailure failure = (TestCaseFailure) testCaseResult;
                final String errorFailure = failure.getExceptionDescription().isAssertError() ? "failure"
                        : "error";
                writer.writeCharacters("\n    ");
                writer.writeStartElement(errorFailure);
                writer.writeAttribute("message",
                        JkUtilsString.escapeHtml(failure.getExceptionDescription().getMessage()));
                writer.writeAttribute("type", failure.getExceptionDescription().getClassName());
                final StringBuilder stringBuilder = new StringBuilder();
                for (final String line : failure.getExceptionDescription().stackTracesAsStrings()) {
                    stringBuilder.append(line).append("\n");
                }
                stringBuilder.append("      ");
                writer.writeCData(stringBuilder.toString());
                writer.writeCharacters("\n    ");
                writer.writeEndElement();
            } else if (testCaseResult instanceof IgnoredCase) {
                writer.writeCharacters("\n    ");
                writer.writeEmptyElement("skipped");
            }
            writer.writeCharacters("\n  ");
            writer.writeEndElement();
        }
    }

}
