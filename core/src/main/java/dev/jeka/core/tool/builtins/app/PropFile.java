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

package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class PropFile {

    private static final String LINE_CONTINUATION_CHAR = "\\";

    final Path path;

    PropFile(Path path) {
        this.path = path;
    }

    private void insertBeforeFirst(String prefix, String lineToInsert) {
        List<String> lines = new LinkedList<>(JkUtilsPath.readAllLines(path));
        int lastMatchingIndex = -1;
        int i=0;
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                lastMatchingIndex = i;
            }
            i++;
        }

        // No such prefix found
        if (lastMatchingIndex == -1) {
            lines.add(""); // cleaner
            lines.add(lineToInsert + "\n");

        } else {

            // track line-breaks (\)
            for (int index=lastMatchingIndex; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line.trim().endsWith("\\")) {
                    lastMatchingIndex++;
                } else {
                    break;
                }
            }
            if (lastMatchingIndex + 1 > lines.size() ) {
                lines.add(lineToInsert);
            } else {
                lines.add(lastMatchingIndex + 1, lineToInsert);
            }
        }
        JkPathFile.of(path).write(String.join( "\n",lines));
    }

    void insertProp(String propKey, String propValue) {
        Properties properties = JkUtilsPath.readPropertyFile(path);
        boolean update = false;
        if (properties.containsKey(propKey)) {
            update = true;
            String existingValue = properties.getProperty(propKey);
            JkLog.warn("Property %s=%s already exists in %s.", propKey, existingValue, path.getFileName());
            String answer =
                    JkPrompt.ask("Do you want to update ? [y/N]");
            if (!"y".equals(answer.toLowerCase())) {
                JkLog.info("Property %s not updated.", propKey);
                return;
            }
        }
        String line = propKey + "=" + propValue;
        // find prefix
        if (propKey.contains(".")) {
            String prefix = JkUtilsString.substringBeforeLast(propKey, ".");
            insertBeforeFirst(prefix, line);
        } else {
            JkPathFile.of(path).write("\n" + line, StandardOpenOption.APPEND);
        }
        JkLog.info("Property %s=%s %s.", propKey, propValue, update ? "updated" : "added");
    }

    void replaceProp(String propKey, String propRawValue) {
        String content = JkPathFile.of(path).readAsString();
        String newContent = updateProperty(content, propKey, propRawValue);
        JkPathFile.of(path).write(newContent, StandardOpenOption.WRITE);
    }

    void appendValueToMultiValuedProp(String propKey, String propValue, String separator, int maxLength) {
        JkProperties props = JkProperties.ofFile(path);
        String fullValue = props.get(propKey);
        if (fullValue == null) {
            this.insertProp(propKey, propValue);
            return;
        }
        List<String>items = Stream.of(fullValue.split(fullValue))
                .map(String::trim)
                .filter(item -> ! JkUtilsString.isBlank(item))
                .collect(Collectors.toCollection(ArrayList::new));
        items.add(propValue);
        String finalValue = String.join(separator, items);
        replaceProp(propKey, finalValue);
    }

    // non-private for testing
    static String updateProperty(String propertiesContent, String key, String newValue) {
        List<String> lines = new LinkedList<>(Arrays.asList(propertiesContent.split("\n")));
        List<String> result = new LinkedList<>();;
        boolean continuation = false;
        for (String line : lines) {
            if (!line.startsWith(key + "=") && !continuation) {
                result.add(line);
                continue;
            }
            continuation = true;
            if (!line.trim().endsWith(LINE_CONTINUATION_CHAR)) {
                continuation = false;
                result.add(key + "=" + newValue);
            }
        }
        return String.join("\n", result);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
