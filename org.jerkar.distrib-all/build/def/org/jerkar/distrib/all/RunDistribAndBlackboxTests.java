package org.jerkar.distrib.all;

import org.jerkar.tool.JkInit;

class RunDistribAndBlackboxTests {

    public static void main(String[] args) {
        JkInit.instanceOf(DistribAllBuild.class, args, "-testSamples=true").doDefault();
    }
}
