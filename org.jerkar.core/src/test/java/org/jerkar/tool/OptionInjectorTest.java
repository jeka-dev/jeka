package org.jerkar.tool;

import java.util.HashMap;
import java.util.Map;

import org.jerkar.tool.JkDoc;
import org.jerkar.tool.OptionInjector;
import org.junit.Assert;
import org.junit.Test;

public class OptionInjectorTest {

    @Test
    public void test() {
	final Map<String, String> map = new HashMap<String, String>();
	map.put("a", "1");
	map.put("b.a", "toto");
	map.put("b.nonExistingField", "bar");
	map.put("c", null);
	final Sample sample = new Sample();
	OptionInjector.inject(sample, map);
	Assert.assertEquals(1, sample.a);
	Assert.assertEquals("toto", sample.b.a);
	Assert.assertTrue(sample.c);
    }

    private static final class Sample {

	@JkDoc("option for a")
	private int a;

	@JkDoc("option for b")
	private B b;

	@JkDoc("option for c")
	private boolean c;

    }

    private static final class B {

	@JkDoc("option for a")
	private String a;

    }

}
