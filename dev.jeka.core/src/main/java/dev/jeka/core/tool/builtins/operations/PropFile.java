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

package dev.jeka.core.tool.builtins.operations;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

class PropFile {

    final Path path;

    PropFile(Path path) {
        this.path = path;
    }

    public void deleteProperty(String propKey) {
        List<String> lines = new LinkedList<>(JkUtilsPath.readAllLines(path));
        for (ListIterator<String> iterator = lines.listIterator(); iterator.hasNext(); ) {
            String line = iterator.next();
            if (line.startsWith(propKey+"=")) {
                iterator.remove();
            }
        }
        JkPathFile.of(path).write(java.lang.String.join( "\n",lines));
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
            lines.add(lineToInsert);

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
        JkPathFile.of(path).write(java.lang.String.join( "\n",lines));
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

}
