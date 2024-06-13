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

package dev.jeka.core.api.utils;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for dealing with files.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsFile {

    /**
     * Returns the content of the specified property file as a
     * {@link Properties} object.
     */
    private static Properties readPropertyFile(Path propertyFile) {
        final Properties props = new Properties();
        try (InputStream fileInputStream = Files.newInputStream(propertyFile)){
            props.load(fileInputStream);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        return props;
    }

    public static Map<String, String> readPropertyFileAsMap(Path propertyFile) {
        final Properties properties = readPropertyFile(propertyFile);
        return JkUtilsIterable.propertiesToMap(properties);
    }

    /**
     * Get the url to the specified file.
     */
    public static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }


}
