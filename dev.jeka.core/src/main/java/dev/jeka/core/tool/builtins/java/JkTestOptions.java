package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.api.java.junit.JkUnit.JunitReportDetail;

/**
 * Options about tests
 */
public final class JkTestOptions {

    /** Turn it on to skip tests. */
    @JkDoc("If true, tests are not run.")
    public boolean skip;

    /** Turn it on to run tests in a withForking process. */
    @JkDoc("If true, tests will be executed in a withForking process.")
    public boolean fork;

    /** Argument passed to the JVM if tests are withForking. Example : -Xms2G -Xmx2G */
    @JkDoc("Argument passed to the JVM if tests are withForking. E.g. -Xms2G -Xmx2G.")
    public String jvmOptions;

    /** Detail level for the test report */
    @JkDoc({ "Detail level of generated report.",
        "BASIC mentions the total duration along details on failed tests.",
        "FULL mentions durations of each tests. ",
    "(e.g. -report=NONE)." })
    public JunitReportDetail report = JunitReportDetail.BASIC;

    /** Turn it on to display System.out and System.err on console while executing tests.*/
    @JkDoc("If true, tests System.out and System.err will be displayed on console.")
    public boolean output;

    @JkDoc("If true, Jerkar runs also test classes suffixed with 'IT'. By default Jerkar runs only test classes " +
            " suffixed with 'Test'.")
    public boolean runIT;

}