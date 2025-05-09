package dev.jeka.core.api.project;

import dev.jeka.core.api.java.JkJavaCompileSpec;

import java.nio.file.Path;
import java.util.List;

public class JkAnnotationProcessorGenerator implements JkProjectSourceGenerator {

    @Override
    public String getDirName() {
        return "annotation-processors";
    }

    @Override
    public void generate(JkProject project, Path generatedSourceDir) {
        List<Path> classpath = project.compilation.resolveDependenciesAsFiles();
        JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of()
                .setEncoding(project.getSourceEncoding())
                .setSources(project.compilation.layout.getSources())
                .setClasspath(classpath)
                .setGeneratedSourceOutputDir(generatedSourceDir);
        project.compilerToolChain.runAnnotationProcessors(project.getJvmTargetVersion(), compileSpec);
    }
}
