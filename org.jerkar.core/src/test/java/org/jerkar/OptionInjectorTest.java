package org.jerkar;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class OptionInjectorTest {

	@Test
	public void test() {
		final Map<String, String> map = new HashMap<String, String>();
		map.put("a", "1");
		map.put("b.a", "toto");
		map.put("b.nonExistingField","bar");
		map.put("c", null);
		final Sample sample = new Sample();
		OptionInjector.inject(sample, map);
		Assert.assertEquals(1, sample.a);
		Assert.assertEquals("toto", sample.b.a);
		Assert.assertTrue(sample.c);
	}

	private static final class Sample {

		@JkOption("option for a")
		private int a;

		@JkOption("option for b")
		private B b;

		@JkOption("option for c")
		private boolean c;

	}

	private static final class B {

		@JkOption("option for a")
		private String a;

	}

}
