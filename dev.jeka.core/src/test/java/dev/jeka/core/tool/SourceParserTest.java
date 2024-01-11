package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("javadoc")
public class SourceParserTest {

    @Test
    public void withOneImport() {
        final JkDependencySet dependencies = parseResource("with-1-inject-classpath.javasource").getDependencies();
        Assert.assertEquals(1, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with3MultiAnnoImports() {
        final JkDependencySet dependencies = parseResource("with-3-inject-classpath.javasource").getDependencies();
        Assert.assertEquals(3, dependencies.getEntries().size());
        JkCoordinateDependency fooBBarDep = dependencies.get("foo:bar");
        System.out.println(fooBBarDep);
    }

    @Test
    public void withoutImport() {
        final JkDependencySet dependencies = parseResource("without-iannotations.javasource").getExportedDependencies();
        Assert.assertEquals(0, dependencies.getEntries().size());
    }

    @Test
    public void with2ProjectImports() {
        final LinkedHashSet<Path> projects = parseResource(("with-2-inject-projects.javasource"))
                .dependencyProjects;
        List<Path> projectList = new LinkedList<>(projects);
        Assert.assertEquals(2, projects.size());
        Assert.assertEquals("dev.jeka.core", projectList.get(0).getFileName().toString());
        Assert.assertEquals("src", projectList.get(1).getFileName().toString());
    }

    @Test
    public void withCompileOptions() {
        final List<String> compileOptions = parseResource(("with-compile-options.javasource"))
                .compileOptions;
        Assert.assertEquals(3, compileOptions.size());
        Assert.assertEquals("-opt1", compileOptions.get(0));
    }

    private static ParsedSourceInfo parseResource(String resourceName) {
        String pathString = SourceParserTest.class.getResource(resourceName).getFile();
        Path file = Paths.get(pathString);
        return SourceParser.parse(file, Paths.get(""));
    }

}
