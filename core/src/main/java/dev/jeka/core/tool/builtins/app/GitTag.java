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

package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.utils.JkUtilsString;

class GitTag implements Comparable<GitTag> {

    private static final String LATEST = "latest";

    public final String value;

    GitTag(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(GitTag o) {
        return JkVersion.of(sanitize(value)).compareTo(JkVersion.of(sanitize(o.value)));
    }

    boolean isVersion() {
        String sanitized = value.startsWith("v") ? value.substring(1) : value;
        if (sanitized.isEmpty()) {
            return false;
        }
        return JkUtilsString.isDigits(sanitized.substring(0, 1));
    }

    private static String sanitize(String value) {
        return value.startsWith("v") ? value.substring(1) : value;
    }

    boolean isLatest() {
        return LATEST.equals(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
