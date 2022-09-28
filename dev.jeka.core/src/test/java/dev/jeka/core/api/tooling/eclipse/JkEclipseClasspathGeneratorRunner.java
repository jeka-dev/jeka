package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.project.JkProject;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkEclipseClasspathGeneratorRunner {

    public static void main(String[] args) throws URISyntaxException {
        final Path zip = Paths.get(JkEclipseClasspathGeneratorIT.class.getResource(JkEclipseClasspathGeneratorIT.ZIP_NAME).toURI());
        JkCoordinateDependency coordinateDependency = JkCoordinateDependency.of("junit:junit:4.11");
        JkFileSystemDependency fileDep = JkFileSystemDependency.of(zip);
        JkProject project = JkProject.of().getConstruction()
                .getCompilation()
                    .configureDependencies(deps -> deps
                            .and(fileDep)
                            .and(coordinateDependency)).__.__;
        JkEclipseClasspathGenerator generator = JkEclipseClasspathGenerator.of(project.getJavaIdeSupport());
        generator.addAttribute(fileDep, "myValue", "myKey");
        generator.addAttribute(fileDep, "myValue2", "myKey2");
        generator.addAttribute(coordinateDependency, "myKey1", "myValue1");
        generator.addAccessRule(fileDep, "nonaccessible", "never/import/this/**");
        generator.setIncludeJavadoc(true);
        generator.setUsePathVariables(true);
        System.out.println(generator.generate());
     }
}

