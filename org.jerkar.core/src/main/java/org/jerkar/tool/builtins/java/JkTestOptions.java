package org.jerkar.tool.builtins.java;

import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.tool.JkDoc;

/**
 * Options about tests
 */
public final class JkTestOptions {

    /** Turn it on to skip tests. */
    @JkDoc("If true, tests are not run.")
    public boolean skip;

    /** Turn it on to run tests in a forked process. */
    @JkDoc("If tyrue, tests will be executed in a forked process.")
    public boolean fork;

    /** Argument passed to the JVM if tests are forked. Example : -Xms2G -Xmx2G */
    @JkDoc("Argument passed to the JVM if tests are forked. Example : -Xms2G -Xmx2G")
    public String jvmOptions;

    /** Detail level for the test report */
    @JkDoc({ "The more details the longer tests take to be processed.",
        "BASIC mention the total time elapsed along detail on failed tests.",
        "FULL detailed report displays additionally the time to run each tests.",
    "Example : -report=NONE" })
    public JunitReportDetail report = JunitReportDetail.BASIC;

    /** Turn it on to display System.out and System.err on console while executing tests.*/
    @JkDoc("If true, tests System.out and System.err will be displayed on console.")
    public boolean output;

}