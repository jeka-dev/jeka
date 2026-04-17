package dev.jeka.core.tool.builtins.intellij;


import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIntellijIml;
import dev.jeka.core.api.tooling.intellij.JkIntelliJImlGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.nio.file.Paths;
import java.util.List;

import static dev.jeka.core.api.depmanagement.JkPopularLibs.*;

class JkImlGeneratorTest {

    @Test
    void withoutJavaProject() {
        JkIntelliJImlGenerator imlGenerator = JkIntelliJImlGenerator.of()
                .setJekaSrcClasspath(JkPathSequence.ofSysPropClassPath())
                .setBaseDir(Paths.get(""));
        JkIntellijIml iml = imlGenerator.computeIml();
        iml.toDoc().print(System.out);
    }

    @Test
    public void withJavaProject() {
        JkProject project = JkProject.of();
        project.compilation.dependencies.modify(deps -> dependencies());
        JkIntelliJImlGenerator imlGenerator = JkIntelliJImlGenerator.of()
                .setIdeSupport(project.getJavaIdeSupport())
                .setJekaSrcClasspath(JkPathSequence.of(JkLocator.getJekaJarPath()));
        JkIntellijIml iml = imlGenerator.computeIml();
        iml.toDoc().print(System.out);
        List<JkIntellijIml.SourceFolder> sourceFolders = iml.component.getContent().getSourceFolders();
        JkIntellijIml.SourceFolder test = sourceFolders.get(3);
        Assertions.assertNull(test.getType());
    }

    @Test
    void withJavaProjectSimpleLayout() {
        JkProject project = JkProject.of();
        project.compilation.dependencies.modify(deps -> dependencies());
        JkIntelliJImlGenerator imlGenerator = JkIntelliJImlGenerator.of()
                .setIdeSupport(project.getJavaIdeSupport())
                .setJekaSrcClasspath(JkPathSequence.of(JkLocator.getJekaJarPath()));
        project.flatFacade.setLayoutStyle(JkCompileLayout.Style.SIMPLE);
        JkIntellijIml iml = imlGenerator.computeIml();
        iml.toDoc().print(System.out);
        List<JkIntellijIml.SourceFolder> sourceFolders = iml.component.getContent().getSourceFolders();
        Assertions.assertEquals(5, sourceFolders.size());
    }

    private JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and(GUAVA.toCoordinate("21.0"))
                .and(JAVAX_SERVLET_API.toCoordinate("3.1.0"))
                .and(JUNIT.toCoordinate("4.11"))
                .and(MOCKITO_ALL.toCoordinate("1.10.19"));
    }

}
