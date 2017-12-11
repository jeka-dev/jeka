package org.jerkar.tool.builtins.idea;


import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.ide.idea.JkImlGenerator;
import org.jerkar.api.project.java.JkJavaProject;
import org.junit.Test;

import java.nio.file.Paths;

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
        JkImlGenerator imlGenerator = new JkImlGenerator(project);
        String result = imlGenerator.generate();
        System.out.println(result);
    }

}
