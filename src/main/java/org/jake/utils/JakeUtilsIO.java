package org.jake.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public final class JakeUtilsIO {

	private JakeUtilsIO() {
	}

	public static PrintStream nopPrintStream() {
		return new PrintStream(nopPrintStream());
	}

	public static OutputStream nopOuputStream() {
		return new OutputStream() {

			@Override
			public void write(int paramInt) throws IOException {
				// Do nothing
			}
		};
	}

}
