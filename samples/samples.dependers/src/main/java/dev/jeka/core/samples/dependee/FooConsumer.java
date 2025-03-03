package dev.jeka.core.samples.dependee;

import com.google.common.collect.ImmutableList;
import dev.jeka.core.samples.Foo;

public class FooConsumer {
	
	private Foo foo = new Foo();
	
	public void consume() {
		ImmutableList.of(); // Check Guava is in the compile path
		foo.display();
	}

}
