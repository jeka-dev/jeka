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

import java.util.Optional;

class BehaviorSettings {

    // Initialized for tests
    static BehaviorSettings INSTANCE = ofDefault();

    final Optional<String> defaultKbeanName;

    final boolean cleanWork;

    final boolean cleanOutput;

    final boolean forceMode; // ignore jeka-src compilation and dep resolution failures

    final boolean skipCompile;

    final String childBase;

    final boolean strict;


    BehaviorSettings(String defaultKbeanName,
                     boolean cleanWork,
                     boolean cleanOutput,
                     boolean forceMode,
                     boolean skipCompile,
                     String childBase,
                     boolean strict) {
        this.defaultKbeanName = Optional.ofNullable(defaultKbeanName);
        this.cleanWork = cleanWork;
        this.cleanOutput = cleanOutput;
        this.forceMode = forceMode;
        this.skipCompile = skipCompile;
        this.childBase = childBase;
        this.strict = strict;
    }

    static BehaviorSettings ofDefault() {
        return new BehaviorSettings(null, false, false, false, false, null, false);
    }

    static void setForceMode() {
        BehaviorSettings forceLodeSettinfgs = new BehaviorSettings(
                INSTANCE.defaultKbeanName.orElse(null),
                INSTANCE.cleanWork,
                INSTANCE.cleanOutput,
                true,
                INSTANCE.skipCompile,
                INSTANCE.childBase,
                INSTANCE.strict);
        INSTANCE = forceLodeSettinfgs;
    }

}
