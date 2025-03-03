package dev.jeka.core.api.java;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class JkJavaCompilerRunner {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkJavaCompilerToolChain.of().compile(JkJavaCompileSpec.of()
                .setOutputDir(Paths.get(JkConstants.OUTPUT_PATH).resolve("bin"))
                .setOption(JkJavaCompileSpec.SOURCE_OPTS, JkJavaVersion.V11.toString()));
    }

}
