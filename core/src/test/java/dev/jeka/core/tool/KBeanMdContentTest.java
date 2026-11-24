package dev.jeka.core.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KBeanMdContentTest {

    @Test
    void toMdContent_ok() {
        JkBeanDescription.MdContent content = JkBeanDescription.of(Md.class, true).toMdContent();
        assertTrue(content.header.contains("Synopsis of Md"));
        assertFalse(content.body.contains("Synopsis of Md"));
        assertTrue(content.body.contains("Multi description."));
        assertTrue(content.body.contains("A\\|B."));
    }

    @JkDoc("Synopsis of Md")
    static class Md extends KBean {

        @JkDoc("Multi description")
        public final JkMultiValue<String> multi = JkMultiValue.of(String.class);

        @JkDoc("A|B")
        public String choice;
    }
}
