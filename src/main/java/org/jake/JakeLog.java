package org.jake;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;

import org.jake.utils.JakeUtilsTime;

public class JakeLog {

	private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<LinkedList<Long>>();

	private static OffsetWriter infoOffsetWriter = new OffsetWriter(new BufferedWriter(new OutputStreamWriter(System.out)));

	private static OffsetWriter warnOffsetWriter = new OffsetWriter(new BufferedWriter(new OutputStreamWriter(System.err)));

	private static OffsetWriter errorOffsetWriter = new OffsetWriter(new BufferedWriter(new OutputStreamWriter(System.err)));

	private static PrintWriter infoWriter = new PrintWriter(infoOffsetWriter, true);

	private static PrintWriter errorWriter = new PrintWriter(errorOffsetWriter, true);

	private static PrintWriter warnWriter = new PrintWriter(warnOffsetWriter, true);


	public static void start(String message) {
		infoWriter.print("- " + message +  " ... " );
		infoOffsetWriter.inc();
		errorOffsetWriter.inc();
		warnOffsetWriter.inc();
		LinkedList<Long> times = START_TIMES.get();
		if (times == null) {
			times = new LinkedList<Long>();
			START_TIMES.set(times);
		}
		times.push(System.nanoTime());

	}

	public static void startAndNextLine(String message) {
		start(message);
		nextLine();
	}

	public static void done() {
		doneMessage("Done");
	}

	public static void done(String message) {
		doneMessage("Done : " + message);
	}

	private static void doneMessage(String message) {
		final LinkedList<Long> times = START_TIMES.get();
		if (times == null || times.isEmpty()) {
			throw new IllegalStateException("This 'done' do no match to any 'start'. "
					+"Please, use 'done' only to mention that the previous 'start' activity is done.");
		}
		final long start = times.poll();
		infoWriter.println(message + " in " + JakeUtilsTime.durationInSeconds(start) + " seconds.");
		infoOffsetWriter.dec();
		errorOffsetWriter.dec();
		warnOffsetWriter.dec();
	}

	public static void info(String message) {
		infoWriter.println(message);
	}

	public static void info(String message, Iterable<String> lines) {
		infoWriter.print(message);
		for (final String line : lines) {
			infoWriter.println(line);
		}
	}

	public static void info(Iterable<String> lines) {
		for (final String line : lines) {
			infoWriter.println(line);
		}
	}

	public static void info(String ... lines) {
		info(Arrays.asList(lines));
	}


	public static void warn(String message) {
		flush();
		warnWriter.println(message);
		warnWriter.flush();
	}

	public static void error(String message) {
		flush();
		errorWriter.println(message);
		errorWriter.flush();
	}

	public static void nextLine() {
		infoWriter.println();
	}

	public static PrintWriter getInfoWriter() {
		return infoWriter;
	}

	public static PrintWriter getWarnWriter() {
		return warnWriter;
	}

	public static PrintWriter getErrorWriter() {
		return errorWriter;
	}

	public static void setWritters(Writer infoWriter, Writer warnWriter, Writer errorWriter) {
		infoOffsetWriter.setDelegate(infoWriter);
		warnOffsetWriter.setDelegate(warnWriter);
		errorOffsetWriter.setDelegate(errorWriter);
	}

	public static void flush() {
		try {
			infoOffsetWriter.delegate.flush();
			warnOffsetWriter.delegate.flush();
			errorOffsetWriter.delegate.flush();
		} catch (final IOException e) {
			throw new RuntimeException("Can't flush log output.");
		}
	}

	public static void offset(int delta) {
		infoOffsetWriter.offsetLevel += delta;
		errorOffsetWriter.offsetLevel += delta;
		warnOffsetWriter.offsetLevel += delta;
	}

	private static class OffsetWriter extends Writer {

		private Writer delegate;

		private int offsetLevel;

		public OffsetWriter(Writer delegate) {
			this.delegate = delegate;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			final String filler = getFiller();
			final int lenght = filler.length();
			if (lenght > 0) {
				delegate.write(filler);
			}
			delegate.write(cbuf, off, len);
		}

		@Override
		public void flush() throws IOException {
			delegate.flush();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		public synchronized void setDelegate(Writer writer) {
			this.delegate = writer;
		}

		private String getFiller() {
			if (offsetLevel == 0) {
				return "";
			}
			if (offsetLevel == 1) {
				return "  ";
			}
			if (offsetLevel == 2) {
				return "    ";
			}
			final StringBuilder result = new StringBuilder("      ");
			for (int i =3; i < offsetLevel;i++) {
				result.append("  ");
			}
			return result.toString();
		}

		public void inc() {
			offsetLevel++;
		}

		public void dec() {
			if (offsetLevel > 0) {
				offsetLevel--;
			}
		}

	}

}
