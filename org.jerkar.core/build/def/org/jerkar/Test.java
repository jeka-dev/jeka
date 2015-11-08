package org.jerkar;

import org.jerkar.api.system.JkLog;

public class Test {

    public static void main(String[] args) {
	JkLog.info("message");
	JkLog.startln("toto");
	JkLog.done();
    }

}
