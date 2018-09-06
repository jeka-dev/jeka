package org.jerkar;

import org.jerkar.tool.JkInit;

class RunBlackboxTests {

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(CoreBuild.class, "-verbose=true").testSamples();
    }
}
