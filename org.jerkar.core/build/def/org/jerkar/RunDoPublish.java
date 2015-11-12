package org.jerkar;

import org.jerkar.tool.JkInit;

class RunDoPublish {

    public static void main(String[] args) {
	JkInit.instanceOf(CoreBuild.class, args).doPublish();
    }
}
