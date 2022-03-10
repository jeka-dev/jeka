package dev.jeka.core.api.marshalling.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Borrowed from VincerDom https://github.com/djeang/vincer-dom
 *
 * Wrapper for {@link Element} offering a Parent-Chaining fluent interface. <p>
 * The underlying element may exist or not. If the underlying element does not exist,
 * a proxy element is used in place but write methods are disabled. The concrete w3c element
 * can be created afterward using the {@link #make()} method.
 *
 * @author Jerome Angibaud
 */
public final class JkDomElement {

    public final JkDomElement __;

    private Element w3cElement;

    private ElementProxy proxyElement;  // only used for non-existing element, so we can create it afterward.

    private JkDomElement(JkDomElement parent, Element element) {
        this.__ = parent;
        this.w3cElement = element;
    }

    private JkDomElement(JkDomElement __, JkDomElement parent, String name) {
        this.__ = __;
        this.proxyElement = ElementProxy.of(parent, name);
    }

    /**
     * Creates a VElement wrapping the specified element.
     */
    public static JkDomElement of(Element element) {
        return new JkDomElement(null, element);
    }

    /**
     * Creates a VElement wrapping the specified parent and element.
     */
    public static <P> JkDomElement of(JkDomElement parent, Element element) {
        return new JkDomElement(parent, element);
    }

    /**
     * Returns the underlying w3cElement. This element can be null if this VElement does not exist.
     */
    public Element getW3cElement() {
        return w3cElement;
    }

    /**
     * Adds the specified attribute name/value on the underlying element.
     * @throws IllegalStateException if the underlying element does not exist.
     */
    public JkDomElement attr(String name, String value) {
        if (value == null) {
            return this;
        }
        assertExist();
        w3cElement.setAttribute(name, value);
        return this;
    }

    /**
     * Returns the value of the specified attribute on this element. Returns <code>null</code> if no such
     * attribute exists.
     */
    public String attr(String name) {
        return w3cElement.getAttribute(name);
    }

    /**
     * Removes the specified attribute of the specified name from the underlying element.
     * @throws IllegalStateException if the underlying element does not exist.
     */
    public JkDomElement removeAttr(String name) {
        assertExist();
        w3cElement.removeAttribute(name);
        return this;
    }

    /**
     * Sets the specified text on the underlying element.
     * @throws IllegalStateException if the underlying element does not exist.
     */
    public JkDomElement text(String text) {
        assertExist();
        w3cElement.setTextContent(text);
        return this;
    }

    /**
     * Returns the text pof the underlying element. <code>null</code> if the underlying element does not exist.
     */
    public String text() {
        if (!exist()) {
            return null;
        }
        return w3cElement.getTextContent();
    }

    /**
     * Adds a child element of the specified name on the underlying element. This method returns the
     * newly created element.
     * @throws IllegalStateException if the underlying element does not exist.
     */
    public JkDomElement add(String name) {
        assertExist();
        Element newElement = w3cElement.getOwnerDocument().createElement(name);
        w3cElement.appendChild(newElement);
        return new JkDomElement(this, newElement);
    }

    /**
     * Returns the first child element of the underlying element having the specified name. <p>
     * If no such element exist, this method returns a proxy element that let creation possible afterward.
     */
    public JkDomElement get(String name) {
        if (!exist()) {
            return ElementProxy.of(this, name).create();
        }
        NodeList nodeList = w3cElement.getElementsByTagName(name);
        if (nodeList.getLength() > 0) {
            return new JkDomElement(this, (Element) nodeList.item(0));
        }
        return new JkDomElement(this, this, name);
    }

    /**
     * Returns an unmodifiable list of the child elements having the specified name and verifying the specified predicate.
     * Returns an empty list if the underlying element does not exist.
     */
    public List<JkDomElement> children(String name, Predicate<JkDomElement> predicate) {
        if (!exist()) {
            return Collections.emptyList();
        }
        List<JkDomElement> result = new LinkedList<>();
        NodeList nodeList = w3cElement.getElementsByTagName(name);
        for (int i = 0; i < nodeList.getLength(); i++) {
            JkDomElement el = new JkDomElement(this, (Element) nodeList.item(i));
            if (predicate.test(el)) {
                result.add(el);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns an unmodifiable list of the child elements having the specified name.
     * Returns an empty list if the underlying element does not exist.
     */
    public List<JkDomElement> children(String name) {
        return children(name, el -> true);
    }

    /**
     * Returns the child first child of this element having the specified name and verifying the specified predicate.
     * Returns <code>null</null> if the underlying element does not exist or no such named child exists.
     */
    public JkDomElement child(String name, Predicate<JkDomElement> predicate) {
        if (!exist()) {
            return null;
        }
        NodeList nodeList = w3cElement.getElementsByTagName(name);
        for (int i = 0; i < nodeList.getLength(); i++) {
            JkDomElement el = new JkDomElement(this, (Element) nodeList.item(i));
            if (predicate.test(el)) {
                return  el;
            }
        }
        return null;
    }

    /**
     * Returns the child first child of this element having the specified name.
     * Returns <code>null</null> if the underlying element does not exist or no such named child exists.
     */
    public JkDomElement child(String name) {
        return child(name, jkDomElement -> true);
    }

    /**
     * Returns an unmodifiable list of elements matching the specified xPath expression.
     */
    public List<JkDomElement> xPath(XPathExpression xPathExpression) {
        List<JkDomElement> result = new LinkedList<>();
        final NodeList nodeList;
        try {
            nodeList = (NodeList) xPathExpression.evaluate(w3cElement, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Error when evaluating xPath expression " + xPathExpression, e);
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            JkDomElement el = new JkDomElement(this, (Element) nodeList.item(i));
            result.add(el);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns an unmodifiable list of elements matching the specified xPath expression.
     */
    public List<JkDomElement> xPath(String xPathExpression) {
        XPathExpression compiledExpression = JkDomXPath.compile(xPathExpression);
        return xPath(compiledExpression);
    }



    /**
     * Adds a sibling element of the specified name just before this one. This method returns the newly
     * created element.
     */
    public JkDomElement addSibling(String name) {
        assertExist();
        Element newElement = w3cElement.getOwnerDocument().createElement(name);
        w3cElement.getParentNode().insertBefore(newElement, w3cElement);
        return JkDomElement.of(this.__, newElement);
    }

    /**
     * Removes the underlying element from its parent children.
     */
    public JkDomElement remove() {
        assertExist();
        this.w3cElement.getParentNode().removeChild(w3cElement);
        return this;
    }

    /**
     * Runs the specified consumer with this element as argument.
     */
    public JkDomElement apply(Consumer<JkDomElement> consumer) {
        assertExist();
        consumer.accept(this);
        return this;
    }

    /**
     * Runs the specified consumer with this element as argument.
     */
    public JkDomElement applyIf(boolean condition, Consumer<JkDomElement> consumer) {
        if (!condition) {
            return this;
        }
        assertExist();
        consumer.accept(this);
        return this;
    }

    /**
     * Returns <code>true</code> if the underlying element exist.
     */
    public boolean exist() {
        return w3cElement != null;
    }

    /**
     * Creates the underlying element and its non-existing parents.
     * Does nothing if the underlying element already exists.
     */
    public JkDomElement make() {
        if (!exist()) {
            this.w3cElement = proxyElement.create().w3cElement;
            this.proxyElement = null;
        }
        return this;
    }

    private void assertExist() {
        if (w3cElement == null) {
            throw new IllegalStateException("Element " + this + " does not exist. " +
                    "Please, invoke #make() prior trying to modify it.");
        }
    }

    private String getName() {
        if (exist()) {
            return w3cElement.getNodeName();
        }
        return proxyElement.name;
    }

    @Override
    public String toString() {
        if (this.__ == null || !(this.__ instanceof JkDomElement))  {
            return getName();
        }
        return this.__ + "/" + getName();
    }

    private static class ElementProxy {

        private final JkDomElement parent;

        private final String name;

        private ElementProxy(JkDomElement parent, String name) {
            this.name = name;
            this.parent = parent;
        }

        static ElementProxy of(JkDomElement parent, String name) {
            return new ElementProxy(parent, name);
        }

        JkDomElement create() {
            parent.make();
            return parent.add(name);
        }

    }


}
