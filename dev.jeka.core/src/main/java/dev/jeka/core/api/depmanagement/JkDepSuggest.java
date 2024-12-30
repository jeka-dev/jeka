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

package dev.jeka.core.api.depmanagement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Tag to let IDE recognise that the value can be suggested with dependency coordinate
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface JkDepSuggest {

    /**
     * We can specify a query to narrow the search, such as "com.google.guava:guava-".
     * <p>
     * An arbitrary list can also be specified by using a comma-separated string,
     * combined with {@code versionOnly=true}.
     * <p>
     * Example:
     * <pre><code>
     * @JkDepSuggest(versionOnly = true, hint = "choice 1,choice 2,choice 3")
     * @JkDepSuggest(versionOnly = true, hint = "com.google.guava:guava-core:")
     * @JkDepSuggest(hint = "com.google.guava:guava-core:")
     * </code></pre>
     */
    String hint() default "";

    /**
     * Mention that we are interested in retrieving versions only.
     * If true, this has to be used in conjunction of {@link #hint()} by specifying a complete
     * group and name as *com.google.guava:guava:*
     */
    boolean versionOnly() default false;
}
