package org.jerkar;

import org.jerkar.tool.JkInit;

class RunSampleTests {

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(CoreBuild.class, "-verbose=true").testSamples();
    }
}
