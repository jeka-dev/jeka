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
 * EXPERIMENTAL
 *
 * Adds one or several sub-bases to the base where is declared the annotated class.
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(JkSubBase.JkSubBases.class)
public @interface JkSubBase {

    /**
     * Specifies the sub-base relative path to import.
     * <p>
     * The path can take the following forms:
     * <ul>
     *   <li>A specific sub-directory: <code>@JkSubBase("foo/bar")</code></li>
     *   <li>Multiple sub-bases discovered from a specified relative path using a wildcard:
     *       <ul>
     *          <li>For example, <code>@JkSubBase("foo/*")</code> will include all sub-directories under <code>foo</code>.</li>
     *          <li><code>@JkSubBase("*")</code> will include all direct child directories.</li>
     *       </ul>
     *   </li>
     * </ul>
     * <p>
     * The discovery process includes:
     * <ol>
     *   <li>Finding direct child folders in the specified path.</li>
     *   <li>Filtering folders, selecting only those that contain either:
     *       <ul>
     *          <li>A <code>jeka.properties</code> file</li>
     *          <li>Or a <code>jeka-src</code> folder</li>
     *       </ul>
     *   </li>
     * </ol>
     */
    String value();

    /**
     * Repeatable container.
     */
    @Target(ElementType.TYPE)
    @interface JkSubBases {

        @JkDepSuggest
        JkSubBase[] value();
    }

}
