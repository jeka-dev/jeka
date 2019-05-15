package org.jerkar;

import org.jerkar.tool.JkInit;

class RunRelease {

    public static void main(String[] args) {
        CoreBuild coreBuild = JkInit.instanceOf(CoreBuild.class, args);
        coreBuild.release();
    }
}
