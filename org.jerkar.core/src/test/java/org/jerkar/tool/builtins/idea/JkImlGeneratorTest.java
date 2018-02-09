package org.jerkar.tool.builtins.idea;


import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.ide.idea.JkImlGenerator;
import org.jerkar.api.project.java.JkJavaProject;
import org.junit.Test;

import java.nio.file.Paths;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.jerkar.api.depmanagement.JkPopularModules.*;

public class JkImlGeneratorTest {

    @Test
    public void withoutJavaProject() {
        JkImlGenerator imlGenerator = new JkImlGenerator(Paths.get(""));
        String result = imlGenerator.generate();
        System.out.println(result);
    }

    @Test
    public void withJavaProject() {
        JkJavaProject project = new JkJavaProject(Paths.get(""));
        project.setDependencies(dependencies());
        project.maker().setDependencyResolver(JkDependencyResolver.of(JkRepo.maven("http://194.253.70.251:8081/nexus/content/groups/multipharma")));
        JkImlGenerator imlGenerator = new JkImlGenerator(project);
        String result = imlGenerator.generate();
        System.out.println(result);
    }

    private JkDependencies dependencies() {
        return JkDependencies.builder()
                .on(GUAVA, "21.0")
                .on(JAVAX_SERVLET_API, "3.1.0", PROVIDED)
                .on(JUNIT, "4.11")
                .on(MOCKITO_ALL, "1.10.19").build();
    }

}
