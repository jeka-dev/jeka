package dev.jeka.core.tool;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("javadoc")
public class FieldInjectorTest {

    @Test
    public void test() {
        final Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b.a", "toto");
        map.put("b.nonExistingField", "bar");
        map.put("c", null);
        final Sample sample = new Sample();
        FieldInjector.inject(sample, map);
        Assert.assertEquals(1, sample.a);
        Assert.assertEquals("toto", sample.b.a);
        Assert.assertFalse(sample.c);
    }

    private static final class Sample {

        @JkDoc("option for a")
        public int a;

        @JkDoc("option for b")
        public B b;

        @JkDoc("option for c")
        public boolean c;

    }

    private static final class B {

        @JkDoc("option for a")
        public String a;

    }

}
