package org.jerkar.tool;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JavaSourceParserTest {

    @Test
    public void withOneImport() {
        final URL resource = JavaSourceParserTest.class.getResource("with1Import.javasource");
        System.out.println("------resource:" + resource);
        final JkDependencies dependencies = JavaSourceParser.of(new File("."), resource).dependencies();
        Assert.assertEquals(1, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with3Imports() {
        final JkDependencies dependencies = JavaSourceParser.of(new File("."),
                JavaSourceParserTest.class.getResource("with3Imports.javasource")).dependencies();
        Assert.assertEquals(3, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void withoutImport() {
        final JkDependencies dependencies = JavaSourceParser.of(new File("."),
                JavaSourceParserTest.class.getResource("withoutImport.javasource")).dependencies();
        Assert.assertEquals(0, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with2ProjectImports() {
        final List<File> projects = JavaSourceParser.of(new File("."),
                JavaSourceParserTest.class.getResource("with2projectImports.javasource"))
                .projects();
        Assert.assertEquals(2, projects.size());
        Assert.assertEquals("org.jerkar.core", projects.get(0).getName());
        Assert.assertEquals("src", projects.get(1).getName());
    }

}
