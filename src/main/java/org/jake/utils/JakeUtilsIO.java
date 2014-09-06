package org.jake.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.jake.file.utils.JakeUtilsFile;

public final class JakeUtilsIO {

	private JakeUtilsIO() {
	}

	/**
	 * Creates a no-op print stream.
	 */
	public static PrintStream nopPrintStream() {
		return new PrintStream(nopOuputStream());
	}

	/**
	 * Creates a no-op outputStream.
	 */
	public static OutputStream nopOuputStream() {
		return new OutputStream() {

			@Override
			public void write(int paramInt) throws IOException {
				// Do nothing
			}
		};
	}

	/**
	 * Close the specified input stream, ignoring any exceptions.
	 */
	public static void closeQuietly(InputStream inputStream) {
		try {
			inputStream.close();
		} catch (final Exception e) {
			// Ignored
		}
	}

	/**
	 * Equivalent to {@link InputStream#read()} but without checked exceptions.
	 */
	public static int read(InputStream inputStream) {
		try {
			return inputStream.read();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	// TODO encoding ????
	public static List<String> readLines(InputStream in) {
		final List<String> result = new LinkedList<String>();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				in));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public static String readResource(String resourcePath) {
		final InputStream is = JakeUtilsFile.class.getClassLoader()
				.getResourceAsStream(resourcePath);
		return readLine(is);
	}

	public static String readLine(InputStream in) {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				in));
		final StringBuilder out = new StringBuilder();
		final String newLine = System.getProperty("line.separator");
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				out.append(line);
				out.append(newLine);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return out.toString();
	}

	public static String readResourceIfExist(String resourcePath) {
		final InputStream is = JakeUtilsFile.class.getClassLoader()
				.getResourceAsStream(resourcePath);
		if (is == null) {
			return null;
		}
		return readLine(is);
	}

	/**
	 * Equivalent to {@link ZipOutputStream#closeEntry()} but without checked exception.
	 */
	public static void closeEntry(ZipOutputStream outputStream) {
		try {
			outputStream.closeEntry();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}




}
