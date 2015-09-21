package org.jerkar.api.tooling;

import java.io.File;

import org.jerkar.api.system.JkProcess;

/**
 * Convenioent class wrapping maven process.
 * 
 * @author Jerome Angibaud
 */
public final class JkMvn implements Runnable {

	/**
	 * Creates a Maven command. Separate argument in different string, don't use white space to separate workds.
	 * Ex : JkMvn.of(myFile, "clean", "install", "-U").
	 */
	public static final JkMvn of(File workingDir, String ... args) {
		final JkProcess jkProcess = JkProcess.ofWinOrUx("mvn.bat", "mvn", args).withWorkingDir(workingDir);
		return new JkMvn(jkProcess);
	}

	private final JkProcess jkProcess;

	private JkMvn(JkProcess jkProcess) {
		super();
		this.jkProcess = jkProcess;
	}

	/**
	 * return a new maven command for this working directory. Separate arguments in different strings, don't use white space to separate workds.
	 * Ex : withCommand("clean", "install", "-U").
	 */
	public final JkMvn commands(String ... args) {
		return new JkMvn(jkProcess.withParameters(args));
	}

	/**
	 * Short hand for #withCommand("clean", "package").
	 */
	public final JkMvn cleanPackage() {
		return commands("clean","package");
	}

	/**
	 * Short hand for #withCommand("clean", "install").
	 */
	public final JkMvn cleanInstall() {
		return commands("clean","install");
	}

	/**
	 * Append a "-U" force update to the list of parameters
	 */
	public final JkMvn forceUpdate(boolean flag) {
		if (flag) {
			return new JkMvn(this.jkProcess.andParameters("-U"));
		}
		return new JkMvn(this.jkProcess.minusParameter("-U"));
	}

	/**
	 * Append or remove a "-X" verbose to the list of parameters
	 */
	public final JkMvn verbose(boolean flag) {
		if (flag) {
			return new JkMvn(this.jkProcess.andParameters("-X"));
		}
		return new JkMvn(this.jkProcess.minusParameter("-X"));
	}

	/**
	 * Returns the underlying process to execute mvn
	 */
	public JkProcess asProcess() {
		return this.jkProcess;
	}

	/**
	 * Shorthand for {@link JkProcess#failOnError(boolean)}
	 */
	public JkMvn failOnError(boolean flag) {
		return new JkMvn(this.jkProcess.failOnError(flag));
	}

	@Override
	public void run() {
		jkProcess.runSync();
	}

}
