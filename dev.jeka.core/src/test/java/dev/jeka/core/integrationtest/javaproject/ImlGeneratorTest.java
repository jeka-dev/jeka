package dev.jeka.core.integrationtest.javaproject;

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator2;
import org.junit.Test;

public class ImlGeneratorTest {

    private static final String VERSION = "2.2.0.RELEASE";

    @Test
    public void generateIml_springbootProject_ok() {

        JkProject project = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-web:" + VERSION)
                )
                .configureTestDeps(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-test:" + VERSION)
                            .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
                ).getProject();
        JkImlGenerator2 imlGenerator = JkImlGenerator2.of().setIdeSupport(project.getJavaIdeSupport());
        String xml = imlGenerator.computeIml().toDoc().toXml();
        System.out.println(xml);
    }

}
