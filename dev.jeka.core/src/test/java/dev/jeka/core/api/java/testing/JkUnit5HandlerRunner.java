package dev.jeka.core.api.java.testing;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkResolveResult;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JkUnit5HandlerRunner {

    public static void main(String[] args) {
        JkLog.setHierarchicalConsoleConsumer();
        //JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(JkDependencySet.of().and("org.junit.vintage:junit-vintage-engine:jar:5.6.0"));
        JkPathSequence pathSquence = resolveResult.getFiles();
        Path path = Paths.get(".idea/output/test") ;
        JkClasspath classpath = JkClasspath.of(path).and(pathSquence);
        JkTestProcessor tp = JkTestProcessor.of();
        tp.launch(classpath, JkTestSelection.of()
                .addIncludeStandardPatterns()
                .addTestClassRoots(path));
    }
}
