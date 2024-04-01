/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
