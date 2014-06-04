package org.jake;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

public class Notifier {
	
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
	}
	
	public static void done() {
		infoWriter.println("Done.");
		infoOffsetWriter.dec();
		errorOffsetWriter.dec();
		warnOffsetWriter.dec();
	} 
	
	public static void done(String message) {
		infoWriter.println("Done : " + message);
		infoOffsetWriter.dec();
		errorOffsetWriter.dec();
		warnOffsetWriter.dec();
	}
	
	public static void info(String message) {
		infoWriter.println(message);
	}
	
	public static void warn(String message) {
		warnWriter.println(message);
	}
	
	public static void error(String message) {
		errorWriter.println(message);
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
	
	private static class OffsetWriter extends Writer {
		
		private Writer delegate;
		
		private int offsetLevel;
		
		public OffsetWriter(Writer delegate) {
			this.delegate = delegate;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			final String filler = getFiller();
			int lenght = filler.length();
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
			StringBuilder result = new StringBuilder("      ");
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
