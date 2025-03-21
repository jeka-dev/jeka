package dev.jeka.core.api.testing;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkException;

/**
 * Template class for performing actions on application to be deployed and undeployed.
 * <p>
 * The typical use-case is for testing applications end-to-end. Users need to implement :
 * <ul>
 *     <li>Methods for starting/stopping the application environment </li>
 *     <li>Method for checking the application is ready</li>
 *     <li>Method to running test</li>
*  </ul>
 *  The {@link #run()} method, then can be invoked to orchestrate the whole execution of tests,
 *  managing de deployment and cleanup of application environment.
 */
public abstract class JkApplicationTester implements Runnable {

    protected int startTimeout = 15*1000;

    protected int reAttemptDelay = 1000;

    private volatile Throwable appStartFailure;

    /**
     * Deploy application environment, run tests and un-deploy the application gracefully.
     */
    public final void run() {
        init();
        JkLog.info("Starting the application...");
        Thread thread = new Thread(this::doStart);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        checkUntilReady();
        JkLog.info("Application started");

        try {
            executeTests();
        } finally {
            JkLog.verbose("Stopping the application...");
            try {
                stopGracefully();
                JkLog.info("Application stopped");
            } catch (Exception e) {
                JkLog.warn("Error while stopping the application:", e.getMessage());
                if (JkLog.isVerbose()) {
                    e.printStackTrace(JkLog.getErrPrintStream());
                }
            }

        }
    }

    /**
     * Prepares the variables required to start and stop the application,
     * including the base URL and port configuration.
     */
    protected void init() {

    }

    /**
     * Starts the application and its environment.
     * This method should be invoked in a separate thread and is expected to block execution.
     * The application should be terminated using the stopXXX methods.
     */
    protected abstract void startApp();

    /**
     * Check and returns if the application is ready to be tested.
     */
    protected abstract boolean isApplicationReady();

    /**
     * Executes the tests for the application under test.
     * The method is supposed to throw an Exception ({@link IllegalStateException for instance} if
     * tests failed.
     */
    protected abstract void executeTests();

    /**
     * This method is invoked when tests are terminated, to undeploy the application and its environment.
     */
    protected void stopGracefully() {
    }

    /**
     * This method is invoked when, the application did not start until defined timeout.
     */
    protected void stopForcefully() {
        stopGracefully();
    }

    /**
     * Retiurn the first free port detected in the dynamic port range.
     */
    protected final int findFreePort() {
        return JkUtilsNet.findFreePort(49152, 65535);
    }

    private void doStart() {
        try {
            startApp();
        } catch (Throwable t) {
            this.appStartFailure = t;
        }
    }

    private void checkUntilReady() {
        long start = System.currentTimeMillis();
        JkLog.verbose("Checking if the application is started...");
        while ( (System.currentTimeMillis() - start) < startTimeout ) {
            if (appStartFailure != null) {
                throw new JkException("Application start failed: " + appStartFailure.getMessage());
            }
            JkLog.verbose("Pinging application...");
            if (isApplicationReady()) {
                return;
            }
            Thread.yield();
            JkLog.verbose("Not yet ready.");
            JkUtilsSystem.sleep(reAttemptDelay);
        }

        // The application did not start on time
        try {
            stopForcefully();
        } catch (RuntimeException e) {
            JkLog.warn("Error while attempting to stop application forcefully : %s", e.getMessage());
        }
        throw new IllegalStateException("Application did not get ready prior timeout of "
                + startTimeout +  " milliseconds");
    }
}
