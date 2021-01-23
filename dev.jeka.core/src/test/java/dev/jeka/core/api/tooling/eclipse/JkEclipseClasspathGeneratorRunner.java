package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.java.project.JkJavaProject;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkEclipseClasspathGeneratorRunner {

    public static void main(String[] args) throws URISyntaxException {
        final Path zip = Paths.get(JkEclipseClasspathGeneratorTest.class.getResource(JkEclipseClasspathGeneratorTest.ZIP_NAME).toURI());
        JkModuleDependency moduleDependency = JkModuleDependency.of("junit:junit:4.11");
        JkFileSystemDependency fileDep = JkFileSystemDependency.of(zip);
        JkJavaProject project = JkJavaProject.of().getConstruction()
                .getDependencyManagement()
                    .addDependencies(JkDependencySet.of()
                            .and(fileDep)
                            .and(moduleDependency)).__.__;
        JkEclipseClasspathGenerator generator = JkEclipseClasspathGenerator.of(project.getJavaIdeSupport());
        generator.addAttribute(fileDep, "myValue", "myKey");
        generator.addAttribute(fileDep, "myValue2", "myKey2");
        generator.addAttribute(moduleDependency, "myKey1", "myValue1");
        generator.addAccessRule(fileDep, "nonaccessible", "never/import/this/**");
        generator.setIncludeJavadoc(true);
        generator.setUsePathVariables(true);
        System.out.println(generator.generate());
     }
}

