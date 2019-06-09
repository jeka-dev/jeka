package dev.jeka.core.tool.builtins.intellij;


import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.ide.intellij.JkImlGenerator;
import dev.jeka.core.api.java.project.JkJavaProject;
import org.junit.Test;

import java.nio.file.Paths;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkPopularModules.*;

public class JkImlGeneratorTest {

    @Test
    public void withoutJavaProject() {
        JkImlGenerator imlGenerator = JkImlGenerator.of(Paths.get(""));
        String result = imlGenerator.generate();
        System.out.println(result);
    }

    @Test
    public void withJavaProject() {
        JkJavaProject project = JkJavaProject.ofMavenLayout(Paths.get(""));
        project.setDependencies(dependencies());
       // project.maker().setDependencyResolver(JkDependencyResolver.of(JkRepo.maven("http://194.253.70.251:8081/nexus/content/groups/multipharma")));
        JkImlGenerator imlGenerator = JkImlGenerator.of(project);
        String result = imlGenerator.generate();
        System.out.println(result);
    }

    private JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and(GUAVA, "21.0")
                .and(JAVAX_SERVLET_API, "3.1.0", PROVIDED)
                .and(JUNIT, "4.11")
                .and(MOCKITO_ALL, "1.10.19");
    }

}
