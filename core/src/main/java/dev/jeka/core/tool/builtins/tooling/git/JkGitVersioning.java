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

package dev.jeka.core.tool.builtins.tooling.git;

import dev.jeka.core.tool.JkDoc;

public class JkGitVersioning {

    private JkGitVersioning() {
    }

    public static JkGitVersioning of() {
        return new JkGitVersioning();
    }

    @JkDoc("If true, a version computed from the current Git branch/tag will be injected into the Maven KBean to " +
            "determine the published version.")
    public boolean enable = false;

    @JkDoc("Some prefer to prefix version tags like 'v1.3.1' instead of simply using '1.3.1'. " +
            "In such cases, this value can be set to 'v' or any other chosen prefix.")
    public String tagPrefix = "";


}
