package dev.jeka.core.tool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KBeanDescriptionTest {

    @Test
    public void readNestedElement_ok() {
        JkBeanDescription desc = JkBeanDescription.of(NestedProp.class, true);
        Assertions.assertEquals(1, desc.beanFields.size());
    }

    static class NestedProp extends KBean {

        public final Options options = new Options();

        @JkDoc  // @JkDoc is mandatory to notify class must be visited to discover nested fields
        public static class Options {
            public String foo = "";
        }

    }

}
