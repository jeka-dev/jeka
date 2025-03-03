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

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDepSuggest;

import java.lang.annotation.*;

/**
 * Adds an element to jeka-src classpath. It can specify a library referenced in a repository (as
 * "com.google.guava:guava:18.0") or file pattern relative to the project
 * directory (as "../lib-folder/mylib.jar" or "libs/*.jar")
 *
 * @author Jerome Angibaud
 * @deprecated Use @JkDep instead
 */
@Deprecated
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(JkInjectClasspath.JkImports.class)
public @interface JkInjectClasspath {

    /**
     * The dependency to import. It can be a module dependency (as "com.google.guava:guava:18.0")
     * or a file dependency (as "../lib-folder.mylib.jar").
     */
    String value();

    /**
     * Repeatable container.
     */
    @Target(ElementType.TYPE)
    @interface JkImports {

        @JkDepSuggest
        JkInjectClasspath[] value();
    }

}
