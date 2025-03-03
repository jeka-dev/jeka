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

package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;

/**
 * Information about problem when resolving dependencies
 */
public class JkModuleDepProblem {

    private final JkModuleId jkModuleId;

    private final JkVersion version;

    private final String problemText;

    private JkModuleDepProblem(JkModuleId jkModuleId, JkVersion version, String problemText) {
        this.jkModuleId = jkModuleId;
        this.version= version;
        this.problemText = problemText;
    }

    public static JkModuleDepProblem of(JkModuleId jkModuleId, String version, String text) {
        return new JkModuleDepProblem(jkModuleId, JkVersion.of(version), text);
    }

    public static JkModuleDepProblem of(JkCoordinate coordinate, String text) {
        return of(coordinate.getModuleId(), coordinate.getVersion().getValue(), text);
    }

    /**
     * Returns the moduleId related to this problem.
     */
    public JkModuleId getModuleId() {
        return jkModuleId;
    }

    /**
     * Returns the version range for which the problematic module dependency has been declared.
     */
    public JkVersion getVersion() {
        return version;
    }

    /**
     * Returns the text explaining this problem.
     */
    public String getProblemText() {
        return problemText;
    }

    @Override
    public String toString() {
        return jkModuleId + ":" + version + " -> " + problemText;
    }
}
