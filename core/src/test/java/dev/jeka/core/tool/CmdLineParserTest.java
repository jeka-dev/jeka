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
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmdLineParserTest {

    @Test
    void nestedEnum_ok() {
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

    @Test
    void fieldValueEqualToMethodName_ok() {
        List<KBeanAction> kBeanActions = parse("project: moduleId=pack pack").toList();
        assertTrue(kBeanActions.stream().anyMatch(action ->
                "moduleId".equals(action.member) && "pack".equals(action.value)));
        assertEquals(1, kBeanActions.stream()
                .filter(action -> action.type == KBeanAction.Action.INVOKE).count());
    }

    @Test
    void fieldValueEqualToFieldName_ok() {
        List<KBeanAction> kBeanActions = parse("project: version=moduleId moduleId=version").toList();
        assertEquals("moduleId", valueOf(kBeanActions, "version"));
        assertEquals("version", valueOf(kBeanActions, "moduleId"));
    }

    @Test
    void fieldWithoutValue_followedByField_fails() {
        CommandLine.ParameterException e = assertThrows(CommandLine.ParameterException.class,
                () -> parse("project: version moduleId=x"));
        assertTrue(e.getMessage().contains("Expected parameter for option 'version'"), e.getMessage());
    }

    @Test
    void fieldWithSeparateValue_ok() {
        assertEquals("1.2", valueOf(parse("project: version 1.2").toList(), "version"));
    }

    @Test
    void booleanWithoutValue_isTrue() {
        assertEquals(Boolean.TRUE, valueOf(parse("project: test.skip").toList(), "test.skip"));
    }

    @Test
    void booleanWithoutValue_followedByField_ok() {
        List<KBeanAction> kBeanActions = parse("project: test.skip moduleId=x").toList();
        assertEquals(Boolean.TRUE, valueOf(kBeanActions, "test.skip"));
        assertEquals("x", valueOf(kBeanActions, "moduleId"));
    }

    @Test
    void booleanWithoutValue_followedByMethod_ok() {
        List<KBeanAction> kBeanActions = parse("project: test.skip pack").toList();
        assertEquals(Boolean.TRUE, valueOf(kBeanActions, "test.skip"));
        assertEquals(1, kBeanActions.stream()
                .filter(action -> action.type == KBeanAction.Action.INVOKE).count());
    }

    @Test
    void booleanFalse_ok() {
        assertEquals(Boolean.FALSE, valueOf(parse("project: test.skip=false").toList(), "test.skip"));
    }

    private static Object valueOf(List<KBeanAction> kBeanActions, String member) {
        return kBeanActions.stream()
                .filter(action -> member.equals(action.member))
                .map(action -> action.value)
                .findFirst().orElse(null);
    }

    private KBeanAction.Container parse(String args) {
        CmdLineArgs cmdArgs = new CmdLineArgs(JkUtilsString.parseCommandline(args));
        return CmdLineParser.parse(cmdArgs, kBeanResolution());
    }

    private static KBeanResolution kBeanResolution() {
        List<String> kbeanClasses = JkBeanDescription.STANDARD_KBEAN_CLASSES.stream()
                .map(Class::getName).collect(Collectors.toList());
        return new KBeanResolution(kbeanClasses, Collections.emptyList(), null, true);
    }


}