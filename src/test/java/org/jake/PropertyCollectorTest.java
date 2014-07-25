package org.jake;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jake.JakeOptions.PropertyCollector;
import org.junit.Test;

public class PropertyCollectorTest  {

	@Test
	public void test() {
		System.getProperties().put("jake.toto", "totovalue");
		final PropertyCollector collector = PropertyCollector.systemProps();
		final String value =collector.stringOr("jake.toto", null, "");
		assertNotNull(value);
		assertNull(collector.stringOr("iiii", null, ""));
	}

}
