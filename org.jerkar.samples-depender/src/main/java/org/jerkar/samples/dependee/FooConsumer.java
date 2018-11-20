package org.jerkar.samples.dependee;

import org.jerkar.samples.Foo;

import com.google.common.collect.ImmutableList;

public class FooConsumer {
	
	private Foo foo = new Foo();
	
	public void consume() {
		ImmutableList.of(); // Check Guava is in the compile path
		foo.display();
	}

}
