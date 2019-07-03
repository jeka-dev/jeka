package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkLog;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkJavadocMakerRunner {

    public static void main(String[] args) throws Exception {
        Path srcDir = Paths.get(JkJavadocMaker.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../../src/main/java").normalize();
        JkPathTreeSet sources = JkPathTreeSet.of(srcDir);
        Path out = Files.createTempDirectory("jekatest");
        JkLog.registerHierarchicalConsoleHandler();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkJavadocMaker.of(sources, out)
                .withClasspath(JkPathTree.of(srcDir.resolve("../../../jeka/libs/provided").normalize()).getFiles())
                .withDisplayOutput(true)
                .process();
        Desktop.getDesktop().open(out.toFile());
    }
}
