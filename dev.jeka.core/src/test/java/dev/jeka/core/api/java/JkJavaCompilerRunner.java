package dev.jeka.core.api.java;

import java.nio.file.Paths;

import javax.tools.ToolProvider;

@SuppressWarnings("javadoc")
public class JkJavaCompilerRunner {

    public static void main(String[] args) {
        System.out.println(JkJavaCompiler.currentJdkSourceVersion());
        System.out.println(ToolProvider.getSystemJavaCompiler().getSourceVersions());

        JkJavaCompiler.ofJdk().compile(JkJavaCompileSpec.of()
                .setOutputDir(Paths.get("jeka/output/bin"))
                .setOption(JkJavaCompileSpec.SOURCE_OPTS, JkJavaVersion.V6.get()));
    }

}
