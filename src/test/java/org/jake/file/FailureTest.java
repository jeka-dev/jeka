package org.jake.file;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FailureTest {
	
	@Test
	public void doAssertFailure() {
		Assert.fail();
	}
	
	@Test
	public void doExceptionraise() {
		throw new RuntimeException();
	}
	
	@Test
	@Ignore
	public void doIgnore() {
		throw new RuntimeException();
	}

}
