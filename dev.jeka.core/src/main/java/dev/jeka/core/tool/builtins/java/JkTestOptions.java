package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.tool.JkDoc;

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

}