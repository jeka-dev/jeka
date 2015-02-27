package org.jake;

import java.io.File;

import org.jake.depmanagement.JakeDependencies;
import org.junit.Assert;
import org.junit.Test;

public class JavaSourceParserTest {

	@Test
	public void withOneImport() {
		final JakeDependencies  dependencies = JavaSourceParser.of(
				new File("."), JavaSourceParserTest.class.getResource("with1Import.javasource")).dependencies();
		Assert.assertEquals(1, dependencies.dependenciesDeclaredWith(Project.JAKE_SCOPE).size());
	}

	@Test
	public void with3Imports() {
		final JakeDependencies  dependencies = JavaSourceParser.of(
				new File("."), JavaSourceParserTest.class.getResource("with3Imports.javasource")).dependencies();
		Assert.assertEquals(3, dependencies.dependenciesDeclaredWith(Project.JAKE_SCOPE).size());
	}



	@Test
	public void withoutImport() {
		final JakeDependencies  dependencies = JavaSourceParser.of(
				new File("."), JavaSourceParserTest.class.getResource("withoutImport.javasource")).dependencies();
		Assert.assertEquals(0, dependencies.dependenciesDeclaredWith(Project.JAKE_SCOPE).size());
	}


}
