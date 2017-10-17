package org.jerkar.api.java;

import java.io.File;
import java.nio.file.Paths;

import javax.tools.ToolProvider;

@SuppressWarnings("javadoc")
public class JkJavaCompilerRunner {

    public static void main(String[] args) {
        System.out.println(JkJavaCompiler.currentJdkSourceVersion());
        System.out.println(ToolProvider.getSystemJavaCompiler().getSourceVersions());

        JkJavaCompiler.base().compile(new JkJavaCompileSpec()
                .setOutputDir(Paths.get("build/output/bin"))
                .setOption(JkJavaCompileSpec.SOURCE_OPTS, JkJavaVersion.V6.name()));
    }

}
