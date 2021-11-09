package dev.jeka.core.integrationtest.javaproject;

import dev.jeka.core.api.project.JkJavaProject;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import org.junit.Test;

public class ImlGeneratorTest {

    private static final String VERSION = "2.2.0.RELEASE";

    @Test
    public void generateIml_springbootProject_ok() {

        JkJavaProject project = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-web:" + VERSION)
                )
                .setTestDependencies(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-test:" + VERSION)
                            .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
                ).getProject();
        JkImlGenerator imlGenerator = JkImlGenerator.of(project.getJavaIdeSupport());
        String xml = imlGenerator.generate();
        System.out.println(xml);
    }

}
