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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropFileTest {

    @Test
    void testUpdateProperty_multiline() throws Exception {
        String content = "aa.1=toto\n" +
                "bb=bla \\\n" +
                "  bb=bla \\\n" +
                "  bb=bla \\\n" +
                "  bb=bla\n" +
                "# A comment \n" +
                "cc=cc\n";
        String result = PropFile.updateProperty(content, "bb", "hoho \\\n gg");
        System.out.println("---------- original content -------------- ");
        System.out.println(content);
        System.out.println("---------- modified content -------------- ");
        System.out.println(result);
        System.out.println("---------- end -------------- ");
    }

    @Test
    void appendValueToMultiValuedProp_appendsOneValueWithoutLeadingSeparator(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("global.properties"), "other=a\n");
        PropFile propFile = new PropFile(file);

        propFile.appendValueToMultiValuedProp("jeka.app.url.trusted", "github.com/a", " ");
        propFile.appendValueToMultiValuedProp("jeka.app.url.trusted", "gitlab.com/c", " ");
        propFile.appendValueToMultiValuedProp("jeka.app.url.trusted", "github.com/e", " ");

        assertEquals("github.com/a gitlab.com/c github.com/e",
                dev.jeka.core.api.system.JkProperties.ofFile(file).get("jeka.app.url.trusted"));
    }

}