package org.jerkar.distrib.all;

import org.jerkar.tool.JkInit;

class RunDoPackageAndBlackboxTests {
    
    public static void main(String[] args) {
	JkInit.instanceOf(DistribAllBuild.class, "-testSamples=true").doDefault();
    }
}
