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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that dynamically computes a required KBean for a KBean declaring the method.
 *
 * The method must be static, take one argument of type {@link JkRunbase},
 * and return a `Class<? extends KBean>` or null.
 *
 * Multiple methods in a single class can use this annotation.
 *
 * <p>Example usage:</p>
 *
 * <pre><code>
 *       @ JkRequire
 *       private static Class<? extends KBean> require(JkRunbase runbase) {
 *           return runbase.getBuildableClass();
 *       }
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JkRequire {

    Class<? extends KBean>[] value() default {};
}
