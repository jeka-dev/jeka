package dev.jeka.core.tool;

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
                "project: scaffold scaffold.template=PROPS layout.style=SIMPLE");
        kBeanActions.forEach(System.out::println);
        Object styleValue = kBeanActions.stream()
                .filter(action -> "layout.style".equals(action.member))
                .map(action -> action.value)
                .findFirst().get();
        assertTrue(styleValue.getClass().isEnum());
    }

    @Test
    public void projectPack_ok() {
        List<KBeanAction> kBeanActions = parse(
                "project: pack");
        kBeanActions.forEach(System.out::println);

    }

    private List<KBeanAction> parse(String args) {
        CmdLineArgs cmdArgs = new CmdLineArgs(JkUtilsString.parseCommandline(args));
        return PicocliParser.parse(cmdArgs, kBeanResolution());
    }

    private static EngineBase.KBeanResolution kBeanResolution() {
        List<String> kbeanClasses = PicocliCommands.STANDARD_KBEAN_CLASSES.stream()
                .map(Class::getName).collect(Collectors.toList());
        return new EngineBase.KBeanResolution(
                kbeanClasses,
                Collections.emptyList(),
                null,
                null);
    }


}