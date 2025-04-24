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

package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

class KBeanInitStore {

    private static final String STORE_FILE_NAME = "kbeans-init.txt";

    public final String defaultKBeanClassName;

    public final List<String> involvedKBeanClassNames;

    KBeanInitStore(String defaultKBeanClassName, List<String> involvedKBeanClassNames) {
        this.defaultKBeanClassName = defaultKBeanClassName;
        this.involvedKBeanClassNames = involvedKBeanClassNames;
    }

    static KBeanInitStore read(Path baseDir) {
        Path storeFile = storeFile(baseDir);
        if (!Files.exists(storeFile)) {
            return ofEmpty();
        }
        JkProperties props = JkProperties.ofFile(storeFile);
        String involvedLine = props.get("involved");
        List<String> involvedKBeanClassNames = Arrays.asList(involvedLine.split(","));
        return new KBeanInitStore(
                JkUtilsString.emptyToNull(props.get("default")),
                involvedKBeanClassNames);
    }

    void store(Path baseDir) {
        Properties properties = new Properties();
        properties.put("default", JkUtilsString.nullToEmpty(defaultKBeanClassName));
        properties.put("involved", String.join(",", involvedKBeanClassNames));
        try (OutputStream out = Files.newOutputStream(storeFile(baseDir))) {
            properties.store(out, "KBean init store");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static KBeanInitStore ofEmpty() {
        return new KBeanInitStore(null, Collections.emptyList());
    }

    private static Path storeFile(Path baseDir) {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(STORE_FILE_NAME);
    }

}
