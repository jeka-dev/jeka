package org.jerkar.api.java.junit;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import dev.jeka.core.api.file.JkPathFile;
import org.jerkar.api.java.junit.JkTestSuiteResult.JkIgnoredCase;
import org.jerkar.api.java.junit.JkTestSuiteResult.JkTestCaseFailure;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

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

    public void writeToFileSystem(Path folder)  {
        JkUtilsPath.createDirectories(folder);
        final Path xmlFile = folder.resolve("TEST-" + result.getSuiteName() + ".xml");
        final Path textFile = folder.resolve(result.getSuiteName() + ".txt");
        try {
            JkPathFile.of(xmlFile).createIfNotExist();
            JkPathFile.of(textFile).createIfNotExist();
            writeXmlFile(xmlFile);
            writeTxtFile(textFile);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeTxtFile(Path txtFile) throws IOException {
        String builder = TEXT_HEAD + "\n" +
                "Test set: " + result.getSuiteName() + "\n" + TEXT_HEAD +
                "\n" + "Tests run: " + result.getRunCount() + ", " +
                "Failures: " + result.assertErrorCount() + ", " +
                "Errors: " + result.getErrorCount() + ", " + "Skipped: " +
                result.getIgnoreCount() + ", " + "Time elapsed: " +
                result.getDurationInMillis() / 1000f + " sec";
        Files.write(txtFile, builder.getBytes());
    }

    private void writeXmlFile(Path xmlFile) throws XMLStreamException, IOException {
        final XMLStreamWriter writer = FACTORY.createXMLStreamWriter(new FileWriter(xmlFile.toFile()));
        writer.writeStartDocument();
        writer.writeCharacters("\n");
        writer.writeStartElement("testsuite");
        writer.writeAttribute("skipped", Integer.toString(result.getIgnoreCount()));
        writer.writeAttribute("tests", Integer.toString(result.getRunCount()));
        writer.writeAttribute("failures", Integer.toString(result.assertErrorCount()));
        writer.writeAttribute("errors", Integer.toString(result.getErrorCount()));
        writer.writeAttribute("name", result.getSuiteName());
        writer.writeAttribute("time", Float.toString(result.getDurationInMillis() / 1000f));
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
        for (final JkTestSuiteResult.JkTestCaseResult testCaseResult : this.result.testCaseResults()) {
            writer.writeCharacters("\n  ");
            writer.writeStartElement("testcase");
            writer.writeAttribute("classname", testCaseResult.getClassName());
            writer.writeAttribute("name", testCaseResult.getTestName());
            if (testCaseResult.getDurationInSecond() != -1) {
                writer.writeAttribute("time", Float.toString(testCaseResult.getDurationInSecond()));
            } else {
                writer.writeAttribute("time", "0.000");
            }
            if (testCaseResult instanceof JkTestCaseFailure) {
                final JkTestCaseFailure failure = (JkTestCaseFailure) testCaseResult;
                final String errorFailure = failure.getExceptionDescription().isAssertError() ? "failure"
                        : "error";
                writer.writeCharacters("\n    ");
                writer.writeStartElement(errorFailure);
                writer.writeAttribute("message",
                        JkUtilsString.escapeHtml(failure.getExceptionDescription().getMessage()));
                writer.writeAttribute("type", failure.getExceptionDescription().getClassName());
                final StringBuilder stringBuilder = new StringBuilder();
                for (final String line : failure.getExceptionDescription().getStackTraceAsStrings()) {
                    stringBuilder.append(line).append("\n");
                }
                stringBuilder.append("      ");
                writer.writeCData(stringBuilder.toString());
                writer.writeCharacters("\n    ");
                writer.writeEndElement();
            } else if (testCaseResult instanceof JkIgnoredCase) {
                writer.writeCharacters("\n    ");
                writer.writeEmptyElement("skipped");
            }
            writer.writeCharacters("\n  ");
            writer.writeEndElement();
        }
    }

}
