package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PicocliHelpTest {

    @Test
    void kbeanHelp_docWithDollarBraces_displayedAsIs() {
        String help = PicocliHelp.kbeanHelp(JkBeanDescription.of(DocVariablesKBean.class, false))
                .getUsageMessage(CommandLine.Help.Ansi.OFF);

        assertTrue(help.contains("Class doc with ${class.var}"), help);
        assertTrue(help.contains("Field doc with ${field.var}"), help);
        assertTrue(help.contains("Method doc with ${method.var}"), help);
        assertFalse(help.contains("null"), help);
    }

    @JkDoc("Class doc with ${class.var}.")
    public static class DocVariablesKBean extends KBean {

        @JkDoc("Field doc with ${field.var}")
        public String foo = "x";

        @JkDoc("Method doc with ${method.var}")
        public void hi() {
        }
    }

    @Test
    void kbeanHelp_undocumentedPostInitOnLongKBeanName_readable() {
        String help = PicocliHelp.kbeanHelp(JkBeanDescription.of(UndocumentedPostInitKBean.class, false))
                .getUsageMessage(CommandLine.Help.Ansi.OFF)
                .replaceAll("\u001B\\[[;\\d]*m", "");  // JkAnsi colors

        assertFalse(help.contains("null"), help);
        assertTrue(help.contains("AVeryLongNameForATargetedKBean Undocumented."), help);
    }

    public static class UndocumentedPostInitKBean extends KBean {

        @JkPostInit
        private void postInit(AVeryLongNameForATargetedKBean kbean) {
        }
    }

    public static class AVeryLongNameForATargetedKBean extends KBean {
    }

    @Test
    void printCmdHelp_defaultKBeanDocWithDollarBraces_displayedAsIs() {
        KBeanResolution resolution = new KBeanResolution(List.of(DocVariablesKBean.class.getName()), List.of(),
                DocVariablesKBean.class.getName(), true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PicocliHelp.printCmdHelp(JkPathSequence.of(), resolution, JkProperties.EMPTY, new PrintStream(out, true));

        String help = out.toString().replaceAll("\u001B\\[[;\\d]*m", "");
        assertTrue(help.contains("Description  : Class doc with ${class.var}"), help);
        assertTrue(help.contains("Standard KBeans"), help);
    }

    @Test
    void kbeanHelp_preInit_listed() {
        String help = PicocliHelp.kbeanHelp(JkBeanDescription.of(PreInitKBean.class, false))
                .getUsageMessage(CommandLine.Help.Ansi.OFF)
                .replaceAll("\u001B\\[[;\\d]*m", "");

        assertTrue(help.contains("Pre-initialize (on activation)"), help);
        assertTrue(help.contains("AVeryLongNameForATargetedKBean Undocumented."), help);
    }

    public static class PreInitKBean extends KBean {

        @JkPreInit
        private static void preInit(AVeryLongNameForATargetedKBean kbean) {
        }
    }

}
