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
        final Map<String, String> map = new HashMap<String, String>();
        map.put("jdk.6", "C:\\UserTemp\\I19451\\software\\jdk1.6.0_24");
        JkJavaCompiler.outputtingIn(new File("build/output/bin"))
        .withSourceVersion(JkJavaCompiler.V6)
        .forkedIfNeeded(map).compile();
    }

}
