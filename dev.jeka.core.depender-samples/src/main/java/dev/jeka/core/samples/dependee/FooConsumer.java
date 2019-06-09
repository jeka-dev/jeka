package dev.jeka.core.samples.dependee;

import dev.jeka.core.samples.Foo;

import com.google.common.collect.ImmutableList;

public class FooConsumer {
	
	private Foo foo = new Foo();
	
	public void consume() {
		ImmutableList.of(); // Check Guava is in the compile path
		foo.display();
	}

}
