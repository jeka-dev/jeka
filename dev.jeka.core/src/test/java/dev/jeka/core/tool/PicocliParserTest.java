package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PicocliParserTest {

    @Test
    public void nestedEnum_ok() {
        List<KBeanAction> kBeanActions = parse(
                "project: scaffold scaffold.template=PROPS layout.style=SIMPLE").toList();
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
        return PicocliParser.parse(cmdArgs, JkProperties.EMPTY, kBeanResolution());
    }

    private static Engine.KBeanResolution kBeanResolution() {
        List<String> kbeanClasses = PicocliCommands.STANDARD_KBEAN_CLASSES.stream()
                .map(Class::getName).collect(Collectors.toList());
        return new Engine.KBeanResolution(
                kbeanClasses,
                Collections.emptyList(),
                null,
                null);
    }


}