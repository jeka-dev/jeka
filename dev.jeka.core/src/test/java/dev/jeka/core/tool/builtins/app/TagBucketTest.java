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

import dev.jeka.core.api.tooling.git.JkGit;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TagBucketTest {

    List<String> tags = Arrays.asList("0.0.1", "0.0.2", "0.0.3", "latest");

    @Test
    public void testHighest() {
        String highest = TagBucket.ofValues(tags).getHighestVersion();
        Assert.assertEquals("0.0.3", highest);
    }

    @Test
    public void testHigherThan() {
        List<String> higherList = TagBucket.ofValues(tags).getHighestVersionsThan("0.0.1");
        Assert.assertEquals(Arrays.asList("0.0.2", "0.0.3"), higherList);
    }

}