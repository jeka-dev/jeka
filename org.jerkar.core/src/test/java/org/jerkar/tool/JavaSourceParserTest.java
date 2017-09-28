package org.jerkar.tool;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JavaSourceParserTest {

    @Test
    public void withOneImport() {
        final URL resource = JavaSourceParserTest.class.getResource("with1import.javasource");
        final JkDependencies dependencies = JavaSourceParser.of(Paths.get(""), resource).dependencies();
        Assert.assertEquals(1, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with3Imports() {
        JavaSourceParser parser = JavaSourceParser.of(Paths.get(""),
                JavaSourceParserTest.class.getResource("with3Imports.javasource"));
        final JkDependencies dependencies = parser.dependencies();
        Assert.assertEquals(3, JkUtilsIterable.listOf(dependencies).size());
        Assert.assertEquals(1, JkUtilsIterable.listOf(parser.importRepos()).size());
    }

    @Test
    public void with3MultiAnnoImports() {
        JavaSourceParser parser = JavaSourceParser.of(Paths.get(""),
                JavaSourceParserTest.class.getResource("with3MultiImports.javasource"));
        final JkDependencies dependencies = parser.dependencies();
        Assert.assertEquals(3, JkUtilsIterable.listOf(dependencies).size());
        Assert.assertEquals(2, JkUtilsIterable.listOf(parser.importRepos()).size());
    }

    @Test
    public void withoutImport() {
        final JkDependencies dependencies = JavaSourceParser.of(Paths.get(""),
                JavaSourceParserTest.class.getResource("withoutImport.javasource")).dependencies();
        Assert.assertEquals(0, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with2ProjectImports() {
        final List<Path> projects = JavaSourceParser.of(Paths.get(""),
                JavaSourceParserTest.class.getResource("with2projectImports.javasource"))
                .projects();
        Assert.assertEquals(2, projects.size());
        Assert.assertEquals("org.jerkar.core", projects.get(0).getFileName().toString());
        Assert.assertEquals("src", projects.get(1).getFileName().toString());
    }

}
