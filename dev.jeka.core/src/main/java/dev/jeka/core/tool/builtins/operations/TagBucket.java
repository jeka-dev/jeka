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

package dev.jeka.core.tool.builtins.operations;

import dev.jeka.core.api.tooling.git.JkGit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class TagBucket {

    private static final String LATEST = "latest";

    final List<JkGit.Tag> tags;

    private TagBucket(List<JkGit.Tag> tags) {
        this.tags = tags;
    }

    static TagBucket of(List<JkGit.Tag> tags) {
       List<JkGit.Tag> result = tags.stream()
               .sorted(JkGit.Tag.VERSION_NAMING_COMPARATOR)
               .collect(Collectors.toList());
       return new TagBucket(result);
    }

    List<JkGit.Tag> getHighestVersions(int count) {
        return tags.stream()
                .sorted(JkGit.Tag.VERSION_NAMING_COMPARATOR)
                .limit(count)
                .collect(Collectors.toList());
    }

    boolean hasTag(String tagName) {
        return tags.stream().map(JkGit.Tag::getPresentableName)
                .anyMatch(name -> name.equals(tagName));
    }

    boolean hasLatest() {
        return tags.stream().anyMatch(tag -> tag.getPresentableName().equals(LATEST));
    }
}
