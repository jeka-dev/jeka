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

package dev.jeka.core.api.project;

import java.nio.file.Path;

public abstract class JkProjectSourceGenerator {

    protected JkProjectSourceGenerator() {
    }

    /**
     * Sources will be generated under the <i>output/generated_sources/[name returned by thus method]</i>
     * This path will be passed as argument to {@link #generate(JkProject, Path)}
     */
    protected abstract String getDirName();

    /**
     * Generates source code under the supplied source directory.
     *
     * @param project            The project for which the sources will be generated.
     * @param generatedSourceDir The dir where the source should be generated
     */
    protected abstract void generate(JkProject project, Path generatedSourceDir);

}
