package org.jerkar.utils;

import java.io.File;

public final class JKUtilsTests {

	/**
	 * A directory to output test fixture.
	 */
	public static final File OUTPUR_DIR = new File("build/output/test-out");

	/**
	 * Returns a file relative having the specified relative path to {@link #OUTPUR_DIR}
	 * @param path
	 * @return
	 */
	public static File tempFile(String path) {
		return new File(OUTPUR_DIR, path);
	}

	private JKUtilsTests() {
		// Can't instantiate
	}



}
