package org.jake.java.test;

import java.io.PrintStream;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitConsoleListener extends RunListener {

	private long startTime;

	private long duration;

	private final PrintStream out;

	public JUnitConsoleListener(PrintStream out) {
		super();
		this.out = out;
	}


	@Override
	public void testStarted(Description description) throws Exception {
		out.println("Start " + description.getClassName() + "." + description.getMethodName() + "...");
		startTime = System.nanoTime();
	}

	@Override
	public void testFinished(Description description) throws Exception {
		duration = (System.nanoTime()-startTime)/1000000;
		out.println("Finished");
		out.println();
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		out.println("Ignored");
		out.println();
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		failure.getException().printStackTrace(out);
		out.println("Assumption failure in " + duration + " ms.");
		out.println();
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		failure.getException().printStackTrace(out);
		out.println("Test failure in " + duration + " ms.");
		out.println();
	}



}
