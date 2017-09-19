package org.jerkar.api.java;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.tools.ToolProvider;

@SuppressWarnings("javadoc")
public class JkJavaCompilerRunner {

    public static void main(String[] args) {
        System.out.println(JkJavaCompiler.currentJdkSourceVersion());
        System.out.println(ToolProvider.getSystemJavaCompiler().getSourceVersions());

        JkJavaCompiler.outputtingIn(new File("build/output/bin"))
        .withOption(JkJavaCompilerSpec.SOURCE_OPTS, JkJavaVersion.V6.name()).compile();
    }

}
