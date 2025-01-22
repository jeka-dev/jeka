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

import dev.jeka.core.api.utils.JkUtilsString;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CmdLineParserTest {

    @Test
    public void nestedEnum_ok() {
        List<KBeanAction> kBeanActions = parse(
                "project: scaffold scaffold.kind=REGULAR layout.style=SIMPLE").toList();
        kBeanActions.forEach(System.out::println);
        Object styleValue = kBeanActions.stream()
                .filter(action -> "layout.style".equals(action.member))
                .map(action -> action.value)
                .findFirst().get();
        assertTrue(styleValue.getClass().isEnum());
    }

    @Test
    public void projectPack_ok() {
        List<KBeanAction> kBeanActions = parse("project: pack").toList();
        kBeanActions.forEach(System.out::println);
    }

    @Test
    public void manyBeanInit_singleInitAction() {
        List<KBeanAction> kBeanActions = parse("project: version=1 project: version=2").toList();
        kBeanActions.forEach(System.out::println);
        assertEquals(2, kBeanActions.size());
    }

    private KBeanAction.Container parse(String args) {
        CmdLineArgs cmdArgs = new CmdLineArgs(JkUtilsString.parseCommandline(args));
        return CmdLineParser.parse(cmdArgs, kBeanResolution());
    }

    private static Engine.KBeanResolution kBeanResolution() {
        List<String> kbeanClasses = JkAllKBeans.STANDARD_KBEAN_CLASSES.stream()
                .map(Class::getName).collect(Collectors.toList());
        return new Engine.KBeanResolution(
                kbeanClasses,
                Collections.emptyList(),
                null,
                null);
    }


}