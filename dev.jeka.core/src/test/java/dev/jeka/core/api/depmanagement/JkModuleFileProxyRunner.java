package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;

public class JkModuleFileProxyRunner {


    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepoSet repos = JkRepo.ofMavenCentral().toSet();
        Path result = JkModuleFileProxy.of(repos, "org.junit.platform:junit-platform-engine:sources:jar:1.7.2")
                .get();
        System.out.println(result);
    }
}