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

package dev.jeka.core.api.system;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalChildFirstClassLoader;
import dev.jeka.core.api.utils.JkUtilsReflect;

public interface JkAnsiConsole {

    static JkAnsiConsole of() {
        return Cache.get(JkProperties.ofStandardProperties());
    }

    void systemInstall();

    void systemUninstall();

    void noAnsi();

    boolean isEnabled();

    JkAnsi ansi();

    class Cache {

        private static JkAnsiConsole CACHED_INSTANCE;

        private static JkAnsiConsole get(JkProperties properties) {
            if (CACHED_INSTANCE != null) {
                return CACHED_INSTANCE;
            }
            String IMPL_CLASS = "dev.jeka.core.api.system.embedded.jansi.JkAnsiConsoleImpl";

            JkCoordinateFileProxy jansiJar = JkCoordinateFileProxy.ofStandardRepos(properties,
                    "org.jline:jansi:3.30.4");
            ClassLoader parentClassloader = JkAnsiConsole.class.getClassLoader();
            JkInternalChildFirstClassLoader childFirstClassLoader = JkInternalChildFirstClassLoader.of(jansiJar.get(),
                    parentClassloader);
            Class<?> clazz = JkClassLoader.of(childFirstClassLoader).load(IMPL_CLASS);
            CACHED_INSTANCE = JkUtilsReflect.invokeStaticMethod(clazz, "of");

            return CACHED_INSTANCE;
        }

    }

}
