package dev.jeka.core.tool.builtins.intellij;


import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;

import static dev.jeka.core.api.depmanagement.JkPopularLibs.*;

public class JkImlGeneratorTest {

    @Test
    public void withoutJavaProject() {
        JkImlGenerator imlGenerator = JkImlGenerator.of()
                .setDefClasspath(JkPathSequence.of(JkClasspath.ofCurrentRuntime()))
                .setBaseDir(Paths.get(""));
        JkIml iml = imlGenerator.computeIml();
        iml.toDoc().print(System.out);
    }

    @Test
    public void withJavaProject() {
        JkProject project = JkProject.of();
        project.getCompilation().configureDependencies(deps -> dependencies());
        JkImlGenerator imlGenerator = JkImlGenerator.of()
                .setIdeSupport(project.getJavaIdeSupport())
                .setDefClasspath(JkPathSequence.of(JkLocator.getJekaJarPath()));
        JkIml iml = imlGenerator.computeIml();
        iml.toDoc().print(System.out);
        List<JkIml.SourceFolder> sourceFolders = iml.getComponent().getContent().getSourceFolders();
        JkIml.SourceFolder test = sourceFolders.get(3);
        Assert.assertNull(test.getType());
    }

    @Test
    public void withJavaProjectSimpleLayout() {
        JkProject project = JkProject.of();
        project.getCompilation().configureDependencies(deps -> dependencies());
        JkImlGenerator imlGenerator = JkImlGenerator.of()
                .setIdeSupport(project.getJavaIdeSupport())
                .setDefClasspath(JkPathSequence.of(JkLocator.getJekaJarPath()));
        project.simpleFacade().useSimpleLayout();
        JkIml iml = imlGenerator.computeIml();
        iml.toDoc().print(System.out);
        List<JkIml.SourceFolder> sourceFolders = iml.getComponent().getContent().getSourceFolders();
        Assert.assertEquals(3, sourceFolders.size());
    }

    private JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and(GUAVA.toCoordinate("21.0"))
                .and(JAVAX_SERVLET_API.toCoordinate("3.1.0"))
                .and(JUNIT.toCoordinate("4.11"))
                .and(MOCKITO_ALL.toCoordinate("1.10.19"));
    }

}
