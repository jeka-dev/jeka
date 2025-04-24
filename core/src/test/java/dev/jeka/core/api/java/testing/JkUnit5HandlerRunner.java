package dev.jeka.core.api.java.testing;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JkUnit5HandlerRunner {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(JkDependencySet.of().and("org.junit.vintage:junit-vintage-engine:jar:5.6.0"));
        JkPathSequence pathSequence = resolveResult.getFiles();
        Path path = Paths.get(".idea/output/test");
        JkPathSequence classpath = JkPathSequence.of(path).and(pathSequence);
        JkTestProcessor tp = JkTestProcessor.of(classpath, path);
        tp.run(JkTestSelection.of().addIncludeMavenPatterns());
    }
}
