package dev.jeka.core.api.marshalling.xml;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Borrowed from VincerDom https://github.com/djeang/vincer-dom
 *
 * Utility class to create {@link XPathExpression} avoiding checked exceptions.
 */
public final class JkDomXPath {

    private JkDomXPath() {
    }

    /**
     * Compiles a string to a {@link XPathExpression}.
     */
    public static XPathExpression compile(String expression) {
        try {
            return XPathFactory.newInstance().newXPath().compile(expression);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Error when compiling xPath expression " + expression, e);
        }
    }

}
