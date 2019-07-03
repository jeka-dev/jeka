package dev.jeka.core.api.java;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Paths;

import javax.tools.ToolProvider;

@SuppressWarnings("javadoc")
public class JkJavaCompilerRunner {

    public static void main(String[] args) {
        //System.out.println(JkJavaCompiler.currentJdkSourceVersion());
        //System.out.println(ToolProvider.getSystemJavaCompiler().getSourceVersions());
        JkLog.registerHierarchicalConsoleHandler();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkJavaCompiler.ofJdk().compile(JkJavaCompileSpec.of()
                .setOutputDir(Paths.get("jeka/output/bin"))
                .setOption(JkJavaCompileSpec.SOURCE_OPTS, JkJavaVersion.V6.get()));
    }

}
