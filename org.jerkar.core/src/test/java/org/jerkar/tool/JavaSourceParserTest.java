package org.jerkar.tool;

import java.io.File;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.junit.Assert;
import org.junit.Test;

public class JavaSourceParserTest {

    @Test
    public void withOneImport() {
	final JkDependencies dependencies = JavaSourceParser
		.of(new File("."), JavaSourceParserTest.class.getResource("with1Import.javasource")).dependencies();
	Assert.assertEquals(1, dependencies.dependenciesDeclaredWith(JkScope.BUILD).size());
    }

    @Test
    public void with3Imports() {
	final JkDependencies dependencies = JavaSourceParser
		.of(new File("."), JavaSourceParserTest.class.getResource("with3Imports.javasource")).dependencies();
	Assert.assertEquals(3, dependencies.dependenciesDeclaredWith(JkScope.BUILD).size());
    }

    @Test
    public void withoutImport() {
	final JkDependencies dependencies = JavaSourceParser
		.of(new File("."), JavaSourceParserTest.class.getResource("withoutImport.javasource")).dependencies();
	Assert.assertEquals(0, dependencies.dependenciesDeclaredWith(JkScope.BUILD).size());
    }

    @Test
    public void with2ProjectImports() {
	final List<File> projects = JavaSourceParser
		.of(new File("."), JavaSourceParserTest.class.getResource("with2projectImports.javasource")).projects();
	Assert.assertEquals(2, projects.size());
	Assert.assertEquals("org.jerkar.core", projects.get(0).getName());
	Assert.assertEquals("src", projects.get(1).getName());
    }

}
