package dev.jeka.core.api.marshalling;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

/**
 * Borrowed from VincerDom https://github.com/djeang/vincer-dom
 *
 * Wrapper for {@link Document} offering a Parent-Chaining fluent interface.
 *
 * @author Jerome Angibaud
 */
public final class JkDomDocument {

    private final Document w3cDocument;

    private JkDomDocument(Document w3cDocument) {
        this.w3cDocument = w3cDocument;
    }

    /**
     * Creates a {@link JkDomDocument} wrapping the specified w3c document.
     */
    public static JkDomDocument of(Document w3cDocument) {
        return new JkDomDocument(w3cDocument);
    }

    /**
     * Creates a document with a root element of the specified name.
     */
    public static JkDomDocument of(String rootName) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc = builder.newDocument();
        Element element = doc.createElement(rootName);
        doc.appendChild(element);
        return new JkDomDocument(doc);
    }

    /**
     * Creates a {@link JkDomDocument} by parsing the content of specified input stream.
     * The stream content is parsed with the specified documentBuilder.
     */
    public static JkDomDocument parse(InputStream inputStream, DocumentBuilder documentBuilder) {
        Document doc;
        try {
            doc = documentBuilder.parse(inputStream);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new JkDomDocument(doc);
    }

    /**
     * Same as {@link #parse(InputStream, DocumentBuilder)} but using a default {@link DocumentBuilder}.
     */
    public static JkDomDocument parse(InputStream inputStream) {
        final DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return parse(inputStream, builder);
    }

    public static JkDomDocument parse(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return parse(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns thd underlying w3c {@link Document}.
     */
    public Document getW3cDocument() {
        return w3cDocument;
    }

    /**
     * Returns the root element of this document.
     */
    public JkDomElement root() {
        Element root = w3cDocument.getDocumentElement();
        return JkDomElement.of(null, root);
    }



    /**
     * Outputs xml in the specified stream.
     */
    public void print(OutputStream out) {
        print(out, configurer -> {});
    }

    public void save(Path file) {
        try (OutputStream os = Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            print(os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Same as {@link #print(OutputStream)} but caller can modify the default XML transformer using the
     * specified {@link Consumer<Transformer>}.
     */
    public void print(OutputStream out, Consumer<Transformer> transformerConfigurer) {
        TransformerFactory tf = TransformerFactory.newInstance();  //NOSONAR
        final Transformer transformer;
        try {
            transformer = tf.newTransformer();  //NOSONAR
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformerConfigurer.accept(transformer);
        try {
            transformer.transform(new DOMSource(w3cDocument),
                    new StreamResult(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public String toXml() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        print(outputStream);
        return new String(outputStream.toByteArray(), Charset.forName("utf-8"));
    }

}
