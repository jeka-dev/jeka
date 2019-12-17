package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkEclipseClasspathGeneratorRunner {

    public static void main(String[] args) throws URISyntaxException {
        final Path zip = Paths.get(JkEclipseClasspathGeneratorTest.class.getResource(JkEclipseClasspathGeneratorTest.ZIP_NAME).toURI());
        JkDependency fileDep = JkFileSystemDependency.of(zip);
        JkModuleDependency moduleDependency = JkModuleDependency.of("junit:junit:4.11");
        JkDependencySet dependencies = JkDependencySet.of()
                .and(fileDep)
                .and(moduleDependency);
        JkEclipseClasspathGenerator generator = JkEclipseClasspathGenerator.of(JkProjectSourceLayout.ofMavenStyle(),
                dependencies,
                JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet()),
                JkJavaVersion.V8);
        generator.addAttribute(fileDep, "myValue", "myKey");
        generator.addAttribute(fileDep, "myValue2", "myKey2");
        generator.addAttribute(moduleDependency, "myKey1", "myValue1");
        generator.addAccessRule(fileDep, "nonaccessible", "never/import/this/**");
        generator.setIncludeJavadoc(true);
        generator.setUsePathVariables(true);
        System.out.println(generator.generate());
     }
}

