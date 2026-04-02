/*
 * Copyright 2014-2026  the original author or authors.
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

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomElement;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JkIjCompilerXml {

    public Path getFilePath() {
        return filePath;
    }

    private final Path filePath;

    private List<Path> processorPath = new ArrayList<>();

    private Map<String, List<String>> javacOptions = new HashMap<>(); // key represents module name

    private JkIjCompilerXml(Path filePath) {
        this.filePath = filePath;
    }

    public static JkIjCompilerXml of(Path filePath) {
        return new JkIjCompilerXml(filePath);
    }

    public static JkIjCompilerXml ofProjectDir(Path dir) {
        return of(dir.resolve(".idea/compiler.xml"));
    }

    public JkIjCompilerXml putJavaecOptions(String moduleName, List<String> options) {
        javacOptions.put(moduleName, options);
        return this;
    }

    public JkIjCompilerXml setProcessorPath(List<Path> processorPath) {
        this.processorPath = processorPath;
        return this;
    }

    public void updateFile() {
        JkPathFile.of(filePath).deleteIfExist();
        toXml().save(filePath);
    }

    public boolean needsUpdate() {
        return !processorPath.isEmpty() || hasJavacOptions();
    }

    private JkDomDocument toXml() {
        var doc = JkDomDocument.of("project");
        doc.root().attr("version", "4");
        if (!this.processorPath.isEmpty()) {
            var profileEl = doc.root()
                .add("component").attr("name", "CompilerConfiguration")
                    .add("annotationProcessing")
                        .add("profile").attr("default", "true").attr("name", "default").attr("enabled", "true");
            this.fillProcessorEl(profileEl);
        }
        if (this.hasJavacOptions()) {
            fillJavacSettings(doc.root());
        }
        return doc;
    }

    private void fillProcessorEl(JkDomElement profileEl) {
        var processorPathEl = profileEl.add("processorPath").attr("useClasspath", "false");
        for (Path entry : processorPath) {
            JkLog.debug("Adding processor %s to compiler processor paths.", entry.getFileName());
            processorPathEl.add("entry").attr("name", entry.toString());
        }
    }

    private void fillJavacSettings(JkDomElement parent) {
        var optionEl =
                parent.add("component").attr("name", "JavacSettings")
                            .add("option").attr("name", "ADDITIONAL_OPTIONS_OVERRIDE");
        for (Map.Entry<String, List<String>> entry : javacOptions.entrySet()) {
            List<String> moduleOptions = entry.getValue();
            if (!moduleOptions.isEmpty()) {
                var moduleEl = optionEl.add("module").attr("name", entry.getKey());
                moduleEl.attr("name", entry.getKey());
                List<String> quotedOptions = moduleOptions.stream()
                        .map(option -> option.contains(" ") ? "\"" + option + "\"" : option)
                        .toList();
                moduleEl.attr("options", String.join(" ", quotedOptions));
            }
        }
    }

    private boolean hasJavacOptions() {
        return this.javacOptions.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().isEmpty());
    }
}
