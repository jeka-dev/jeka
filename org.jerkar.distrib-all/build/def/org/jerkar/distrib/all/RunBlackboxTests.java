package org.jerkar.distrib.all;

import org.jerkar.tool.JkInit;

class RunBlackboxTests {

    public static void main(String[] args) {
        JkInit.instanceOf(DistribAllBuild.class, "-verbose=true").testSamples();
    }
}
