package org.jerkar;

import org.jerkar.tool.JkInit;

class RunPublishLocal {

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).javaPlugin.publishLocal();
    }
}
