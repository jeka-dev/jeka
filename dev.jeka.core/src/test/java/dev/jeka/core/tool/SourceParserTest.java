package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("javadoc")
@JkInjectCompileOption("-opt1 -opt2 ")
@JkInjectCompileOption("opt3")
public class SourceParserTest {

    @Test
    public void withOneImport() {
        final URL resource = SourceParserTest.class.getResource("with-1-import.javasource");
        final JkDependencySet dependencies = SourceParser.parse(Paths.get(""), resource).dependencies;
        Assert.assertEquals(1, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with3MultiAnnoImports() {
        ParsedSourceInfo parsedSourceInfo = SourceParser.parse(Paths.get(""),
                SourceParserTest.class.getResource("with-3-multi-Imports.javasource"));
        final JkDependencySet dependencies = parsedSourceInfo.dependencies;
        Assert.assertEquals(3, dependencies.getEntries().size());
    }

    @Test
    public void withoutImport() {
        final JkDependencySet dependencies = SourceParser.parse(Paths.get(""),
                SourceParserTest.class.getResource("without-import.javasource")).dependencies;
        Assert.assertEquals(0, dependencies.getEntries().size());
    }

    @Test
    public void with2ProjectImports() {
        final LinkedHashSet<Path> projects = SourceParser.parse(Paths.get(""),
                SourceParserTest.class.getResource("with-2-project-imports.javasource"))
                .dependencyProjects;
        List<Path> projectList = new LinkedList<>(projects);
        Assert.assertEquals(2, projects.size());
        Assert.assertEquals("dev.jeka.core", projectList.get(0).getFileName().toString());
        Assert.assertEquals("src", projectList.get(1).getFileName().toString());
    }

}
