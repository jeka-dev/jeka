package org.jake;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;

import org.jake.utils.JakeUtilsTime;

public class JakeLog {

	private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<LinkedList<Long>>();

	private static OffsetStream infoWriter = new OffsetStream(System.out);

	private static OffsetStream errorWriter = new OffsetStream(System.err);

	private static OffsetStream warnWriter = new OffsetStream(System.err);

	public static void start(String message) {
		infoWriter.print("- " + message +  " ... " );
		infoWriter.inc();
		errorWriter.inc();
		warnWriter.inc();
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
		infoWriter.dec();
		errorWriter.dec();
		warnWriter.dec();
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

	public static void warn(Iterable<String> lines) {
		for (final String line : lines) {
			warnWriter.println(line);
		}
	}


	public static void warn(String message) {
		flush();
		warnWriter.println(message);
		warnWriter.flush();
	}

	public static void warnIf(boolean condition, String message) {
		if (condition) {
			warn(message);
		}
	}

	public static void error(String message) {
		flush();
		errorWriter.println(message);
		errorWriter.flush();
	}

	public static void nextLine() {
		infoWriter.println();
	}

	public static PrintStream infoStream() {
		return infoWriter;
	}

	public static PrintStream warnStream() {
		return warnWriter;
	}

	public static PrintStream errorStream() {
		return errorWriter;
	}



	public static void flush() {
		infoWriter.flush();
		warnWriter.flush();
		errorWriter.flush();
	}

	public static void offset(int delta) {
		infoWriter.offsetLevel += delta;
		errorWriter.offsetLevel += delta;
		warnWriter.offsetLevel += delta;
	}

	public static int offset() {
		return infoWriter.offsetLevel;
	}


	private static class OffsetStream extends PrintStream {

		private int offsetLevel;

		public OffsetStream(PrintStream delegate) {
			super(delegate);
		}

		@Override
		public void write(byte[] cbuf, int off, int len)  {
			final byte[] filler = getFiller().getBytes();
			final int lenght = filler.length;
			if (lenght > 0) {
				super.write(filler,0, lenght);
			}
			super.write(cbuf, off, len);
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
