package org.jake.java.test.junit;

import java.io.PrintStream;

import org.jake.JakeLog;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitConsoleListener extends RunListener {

	private PrintStream out;

	private PrintStream err;

	@Override
	public void testStarted(Description description) throws Exception {
		JakeLog.start("Running " + description.getClassName() + "." + description.getMethodName() );
		out = System.out;
		err = System.err;
		System.setOut(JakeLog.infoStream());
		System.setErr(JakeLog.errorStream());
	}

	@Override
	public void testFinished(Description description) throws Exception {
		JakeLog.done();
		System.setOut(out);
		System.setErr(err);
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		JakeLog.info("- Test " + description.getDisplayName() + " ignored.");
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		failure.getException().printStackTrace(JakeLog.infoStream());
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		failure.getException().printStackTrace(JakeLog.infoStream());;
	}



}
