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
public class JavaEngineSourceParserTest {

    @Test
    public void withOneImport() {
        final URL resource = JavaEngineSourceParserTest.class.getResource("with1import.javasource");
        final JkDependencySet dependencies = EngineSourceParser.of(Paths.get(""), resource).dependencies();
        Assert.assertEquals(1, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with3Imports() {
        EngineSourceParser parser = EngineSourceParser.of(Paths.get(""),
                JavaEngineSourceParserTest.class.getResource("with3Imports.javasource"));
        final JkDependencySet dependencies = parser.dependencies();
        Assert.assertEquals(3, dependencies.getEntries().size());
    }

    @Test
    public void with3MultiAnnoImports() {
        EngineSourceParser parser = EngineSourceParser.of(Paths.get(""),
                JavaEngineSourceParserTest.class.getResource("with3MultiImports.javasource"));
        final JkDependencySet dependencies = parser.dependencies();
        Assert.assertEquals(3, dependencies.getEntries().size());
    }

    @Test
    public void withoutImport() {
        final JkDependencySet dependencies = EngineSourceParser.of(Paths.get(""),
                JavaEngineSourceParserTest.class.getResource("withoutImport.javasource")).dependencies();
        Assert.assertEquals(0, dependencies.getEntries().size());
    }

    @Test
    public void with2ProjectImports() {
        final LinkedHashSet<Path> projects = EngineSourceParser.of(Paths.get(""),
                JavaEngineSourceParserTest.class.getResource("with2projectImports.javasource"))
                .projects();
        List<Path> projectList = new LinkedList<>(projects);
        Assert.assertEquals(2, projects.size());
        Assert.assertEquals("dev.jeka.core", projectList.get(0).getFileName().toString());
        Assert.assertEquals("src", projectList.get(1).getFileName().toString());
    }

}
