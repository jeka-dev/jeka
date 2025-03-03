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

import java.lang.annotation.*;

/**
 * Specifies the options of the Jeka compiler for jeka-src classes.
 * Normally, one option by annotation is expected tough this annotation is repeatable.
 * You can declare it has follow :
 * <pre><code>
 * @JkCompileOption("-deprecation")
 * @JkCompileOption({"-processorPath", "/foo/bar"})
 * </code></pre>
 *
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(JkCompileOption.JkCompileOptions.class)
public @interface JkCompileOption {

    /**
     * The dependency to import. It can be a module dependency (as "com.google.guava:guava:18.0")
     * or a file dependency (as "../lib-folder.mylib.jar").
     */
    String[] value();


    /**
     * Repeatable container.
     */
    @Target(ElementType.TYPE)
    @interface JkCompileOptions {
        JkCompileOption[] value();
    }

}