package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


class SourceParserTest {

    @Test
    public void withOneImport() {
        final JkDependencySet dependencies = parseResource("with-1-inject-classpath.javasource").getDependencies();
        assertEquals(1, JkUtilsIterable.listOf(dependencies).size());
    }

    @Test
    public void with3MultiAnnoImports() {
        final JkDependencySet dependencies = parseResource("with-3-inject-classpath.javasource").getDependencies();
        assertEquals(3, dependencies.getEntries().size());
        JkCoordinateDependency fooBBarDep = dependencies.get("foo:bar");
        System.out.println(fooBBarDep);
    }

    @Test
    public void withoutImport() {
        final JkDependencySet dependencies = parseResource("without-iannotations.javasource").getExportedDependencies();
        assertEquals(0, dependencies.getEntries().size());
    }

    @Test
    public void with2ProjectImports() {
        final LinkedHashSet<Path> projects = parseResource(("with-2-inject-projects.javasource"))
                .importedBaseDirs;
        List<Path> projectList = new LinkedList<>(projects);
        assertEquals(2, projects.size());
        assertEquals("dev.jeka.core", projectList.get(0).getFileName().toString());
        assertEquals("src", projectList.get(1).getFileName().toString());
    }

    @Test
    public void withCompileOptions() {
        final List<String> compileOptions = parseResource(("with-compile-options.javasource"))
                .compileOptions;
        assertEquals(3, compileOptions.size());
        assertEquals("-opt1", compileOptions.get(0));
    }

    private static ParsedSourceInfo parseResource(String resourceName) {
        String pathString = SourceParserTest.class.getResource(resourceName).getFile();
        if (JkUtilsSystem.IS_WINDOWS && pathString.startsWith("/")) {
            pathString = pathString.substring(1);
        }
        Path file = Paths.get(pathString);
        return SourceParser.parse(file, Paths.get(""));
    }

}
