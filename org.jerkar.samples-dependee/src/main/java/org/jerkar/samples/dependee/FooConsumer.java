package org.jerkar.samples.dependee;

import org.jerkar.samples.Foo;

public class FooConsumer {
	
	private Foo foo = new Foo();
	
	public void consume() {
		foo.display();
	}

}
