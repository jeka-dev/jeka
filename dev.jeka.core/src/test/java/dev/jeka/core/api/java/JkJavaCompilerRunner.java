package dev.jeka.core.api.java;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class JkJavaCompilerRunner {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkJavaCompiler.of().compile(JkJavaCompileSpec.of()
                .setOutputDir(Paths.get("jeka/output/bin"))
                .setOption(JkJavaCompileSpec.SOURCE_OPTS, JkJavaVersion.V11.get()));
    }

}
