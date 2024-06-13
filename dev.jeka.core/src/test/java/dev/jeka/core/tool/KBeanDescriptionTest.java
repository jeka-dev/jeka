package dev.jeka.core.tool;

import org.junit.Assert;
import org.junit.Test;

public class KBeanDescriptionTest {

    @Test
    public void readNestedElement_ok() {
        KBeanDescription desc = KBeanDescription.of(NestedProp.class, false);
        Assert.assertEquals(1, desc.beanFields.size());
    }

    static class NestedProp extends KBean {

        public final Options options = new Options();

        @JkDoc  // @JkDoc is mandatory to notify class must be visited to discover nested fields
        public static class Options {
            public String foo = "";
        }

    }

}
