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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.system.JkLog;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class JkUtilsJdkTest {

    @Test
    @Ignore
    public void testGetJdk() {
        JkLog.setDecorator(JkLog.Style.INDENT);
        Path path = JkUtilsJdk.getJdk("graalvm", "22");
        Assert.assertTrue(Files.exists(path.resolve("bin")));
    }
}