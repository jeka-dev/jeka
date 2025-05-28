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

package dev.jeka.core.api.marshalling.json;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalChildFirstClassLoader;
import dev.jeka.core.api.system.JkAnsiConsole;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;

/**
 * Interface representing a utility for JSON serialization and deserialization.
 * Provides methods for converting objects to JSON and parsing JSON strings into objects.
 */
public interface JkJson {

    static JkJson of() {
        return JkJson.Cache.get(JkProperties.ofStandardProperties());
    }

    /**
     * Parses a JSON string and converts it into an object of the specified type.
     *
     * @param <T> the type of the object to return
     * @param json the JSON string to be parsed
     * @param valueType the class representing the type of the object to return. It can be a generic Map witch
     *                  leads in Map<String, Object> where numbers are mapped to `double`.
     * @return an object of the specified type, created from the JSON string
     */
    <T> T parse(String json, Class<T> valueType);

    /**
     * Converts the given object into its JSON string representation.
     *
     * @param value the object to be serialized to JSON
     * @return a JSON string representing the given object
     */
    String toJson(Object value);

    class Cache {

        private static JkJson CACHED_INSTANCE;

        private static JkJson get(JkProperties properties) {
            if (CACHED_INSTANCE != null) {
                return CACHED_INSTANCE;
            }
            String IMPL_CLASS = "dev.jeka.core.api.marshalling.json.embedded.gson.JkJsonImpl";

            // Another version of classGraph may be present on the classpath
            // Some libraries as org.webjars:webjars-locator-core use it.
            // For this library version we need to create a dedicated classloader
            // with child-first strategy.
            JkCoordinateFileProxy jansiJar = JkCoordinateFileProxy.ofStandardRepos(properties,
                    "com.google.code.gson:gson:2.13.1");
            ClassLoader parentClassloader = JkAnsiConsole.class.getClassLoader();
            JkInternalChildFirstClassLoader childFirstClassLoader = JkInternalChildFirstClassLoader.of(jansiJar.get(),
                    parentClassloader);
            Class<?> clazz = JkClassLoader.of(childFirstClassLoader).load(IMPL_CLASS);
            CACHED_INSTANCE = JkUtilsReflect.invokeStaticMethod(clazz, "of");

            return CACHED_INSTANCE;
        }

    }
}
