/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomElement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JkImlReader {

    private final JkDomDocument doc;

    private JkImlReader(JkDomDocument doc) {
        this.doc = doc;
    }

    public static JkIntellijJdk getJdk(Path imlFile) {
        JkImlReader imlReader = JkImlReader.of(imlFile);
        if (imlReader == null) {
            return null;
        }
        return imlReader.specificJdkVersion();

    }

    public static JkImlReader of(Path imlFile) {
        if (!Files.exists(imlFile)) {
            return null;
        }
        try {
            return new JkImlReader(JkDomDocument.parse(imlFile));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public JkIntellijJdk specificJdkVersion() {
        List<JkDomElement> els =doc.root().xPath("/module/component/orderEntry[@type='inheritedJdk']");
        if (!els.isEmpty()) {
            return JkIntellijJdk.ofInherited();
        }

        els =doc.root().xPath("/module/component/orderEntry[@type='jdk' and @jdkType='JavaSDK']");
        if (els.isEmpty()) {
            return null;
        }
        JkDomElement el = els.get(0);
        String jdkName = el.attr("jdkName");
        return JkIntellijJdk.of(jdkName);
    }
}
