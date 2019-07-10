package dev.jeka.core;

import dev.jeka.core.tool.JkInit;

public class RunBuildAndIT {

    public static void main(String[] args) {
        CoreBuild coreBuild = JkInit.instanceOf(CoreBuild.class, args);
        coreBuild.javaPlugin.tests.runIT = true;
        coreBuild.javaPlugin.pack();
    }
}
