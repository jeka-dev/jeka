package dev.jeka.core.tool;

import org.junit.jupiter.api.Test;

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

}
