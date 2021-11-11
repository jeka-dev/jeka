package dev.jeka.core.tool.builtins.intellij;


import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import org.junit.Test;

import java.nio.file.Paths;

import static dev.jeka.core.api.depmanagement.JkPopularModules.*;

public class JkImlGeneratorTest {

    @Test
    public void withoutJavaProject() {
        JkImlGenerator imlGenerator = JkImlGenerator.of(JkIdeSupport.of(Paths.get("")));
        String result = imlGenerator.generate();
        System.out.println(result);
    }

    @Test
    public void withJavaProject() {
        JkProject project = JkProject.of();
        project.getConstruction().getCompilation().setDependencies(deps -> dependencies());
        JkImlGenerator imlGenerator = JkImlGenerator.of(project.getJavaIdeSupport());
        String result = imlGenerator.generate();
        System.out.println(result);
    }

    private JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and(GUAVA.version("21.0"))
                .and(JAVAX_SERVLET_API.version("3.1.0"))
                .and(JUNIT.version("4.11"))
                .and(MOCKITO_ALL.version("1.10.19"));
    }

}
