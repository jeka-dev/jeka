package org.jerkar.distrib.all;

import org.jerkar.tool.JkInit;

class RunDistribAndBlackboxTests {

    public static void main(String[] args) {
        JkInit.instanceOf(OldDistribAllBuild.class, args, "-testSamples=true").doDefault();
    }
}
