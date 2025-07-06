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

package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;

import javax.print.Doc;
import java.nio.file.Path;

class IntelliJKotlincXml {

    String additionalArguments = "";

    String apiVersion = "2.1";

    String kotlinVersion = "2.2.0";

    JkJavaVersion jvmVersion;

    JkDomDocument toDoc() {
        return JkDomDocument.of("project").root().attr("version", "4")
                .add("component").attr("name", "Kotlin2JsCompilerArguments")
                    .add("option").attr("name", "moduleKind").attr("value", "plain").__.__
                .add("component").attr("name", "Kotlin2JvmCompilerArguments")
                    .add("option").attr("name", "jvmTarget").attr("value", jvmVersion.toString()).__.__
                .add("component").attr("name", "KotlinCommonCompilerArguments")
                    .add("option").attr("name", "apiVersion").attr("value", apiVersion).__
                    .add("option").attr("name", "languageVersion").attr("value", apiVersion).__.__
                .add("component").attr("name", "KotlinCompilerSettings")
                    .add("option").attr("name", "KotlinCompilerSettings").attr("value", additionalArguments).__.__
                .add("component").attr("name", "KotlinJpsPluginSettings")
                    .add("option").attr("name", "version").attr("value", kotlinVersion)
                .getDoc();
    }

    void save(Path path) {
        toDoc().save(path);
    }

}
