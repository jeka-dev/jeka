package org.jerkar.api.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for dealing with Inputs/Outputs.
 * 
 * @author Jerome Angibaud
 */
public final class JkUtilsIO {

	private JkUtilsIO() {
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

	@SuppressWarnings("unchecked")
	public static List<ZipEntry> zipEntries(ZipFile zipFile) {
		final List<ZipEntry> result = new LinkedList<ZipEntry>();
		final Enumeration<ZipEntry> en = (Enumeration<ZipEntry>) zipFile.entries();
		while(en.hasMoreElements()) {
			result.add(en.nextElement());
		}
		return result;
	}



	/**
	 * Closes the specified input stream, ignoring any exceptions.
	 */
	public static void closeQuietly(InputStream ...inputStreams) {
		for (final InputStream inputStream : inputStreams) {
			try {
				inputStream.close();
			} catch (final Exception e) {
				// Ignored
			}
		}
	}

	/**
	 * Closes the specified writer, ignoring any exceptions.
	 */
	public static void closeQuietly(Writer ... writers) {
		for (final Writer writer : writers) {
			try {
				writer.close();
			} catch (final Exception e) {
				// Ignored
			}
		}
	}

	/**
	 * Closes the specified reader, ignoring any exceptions.
	 */
	public static void closeQuietly(Reader ...readers) {
		for (final Reader reader : readers) {
			try {
				reader.close();
			} catch (final Exception e) {
				// Ignored
			}
		}
	}

	/**
	 * Same as {@link FileInputStream} constructor but throwing unchecked exceptions.
	 */
	public static FileInputStream inputStream(File file) {
		try {
			return new FileInputStream(file);
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException("File " + file + " not found.", e);
		}
	}

	/**
	 * Same as {@link FileOutputStream} constructor but throwing unchecked exceptions.
	 */
	public static FileOutputStream outputStream(File file, boolean append) {
		try {
			return new FileOutputStream(file, append);
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException("File " + file + " not found.", e);
		}
	}

	/**
	 * Same as {@link ZipFile#getInputStream(ZipEntry)} but throwing only unchecked exceptions.
	 */
	public static InputStream inputStream(ZipFile zipFile, ZipEntry entry) {
		try {
			return zipFile.getInputStream(entry);
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException("File " + zipFile + " not found.", e);
		} catch (final IOException e) {
			throw new RuntimeException("File " + zipFile + " not found.", e);
		}
	}

	/**
	 * Same as {@link URL#openStream()} but throwing only unchecked exceptions.
	 */
	public static InputStream inputStream(URL file) {
		try {
			return file.openStream();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Close the specified object input, ignoring any exceptions.
	 */
	public static void closeQuietly(ObjectInput ... objectInputs) {
		for (final ObjectInput objectInput : objectInputs) {
			try {
				objectInput.close();
			} catch (final Exception e) {
				// Ignored
			}
		}
	}


	/**
	 * Close the specified output stream, ignoring any exceptions.
	 */
	public static void closeQuietly(OutputStream ... outputStreams) {
		for(final OutputStream outputStream : outputStreams) {
			try {
				outputStream.close();
			} catch (final Exception e) {
				// Ignored
			}
		}
	}

	/**
	 * Equivalent to {@link InputStream#read()} but throwing only unchecked exceptions.
	 */
	public static int read(InputStream inputStream) {
		try {
			return inputStream.read();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the content of the specified input stream, line by line.
	 */
	// TODO encoding ????
	public static List<String> readAsLines(InputStream in) {
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

	/**
	 * Returns the content of the given url as a string.
	 */
	public static String read(URL url) {
		InputStream is;
		try {
			is = url.openStream();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		try {
			return readAsString(is);
		} finally {
			closeQuietly(is);
		}
	}

	/**
	 * Returns the content of the given input stream as a single string.
	 */
	public static String readAsString(InputStream in) {
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

	/**
	 * Returns the content of the given resource as string if exist. Otherwise returns <code>null</code>.
	 */
	public static String readResourceIfExist(String resourcePath) {
		final InputStream is = JkUtilsFile.class.getClassLoader()
				.getResourceAsStream(resourcePath);
		if (is == null) {
			return null;
		}
		return readAsString(is);
	}

	/**
	 * Equivalent to {@link ZipOutputStream#closeEntry()} but without checked
	 * exception.
	 */
	public static void closeEntry(ZipOutputStream ...outputStreams) {
		for (final ZipOutputStream outputStream : outputStreams) {
			try {
				outputStream.closeEntry();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Creates a {@link ZipOutputStream} from a given file (existing or not).
	 */
	public static ZipOutputStream createZipOutputStream(File file,
			int compressLevel) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			final FileOutputStream fos = new FileOutputStream(file);
			final ZipOutputStream zos = new ZipOutputStream(fos);
			zos.setLevel(compressLevel);
			return zos;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Writes all the entries from a given ZipFile to the specified {@link ZipOutputStream}.
	 */
	public static Set<String> mergeZip(ZipOutputStream zos, ZipFile zipFile) {
		final Set<String> duplicateEntries = new HashSet<String>();
		final Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			final ZipEntry e = entries.nextElement();
			try {
				if (!e.isDirectory()) {
					final boolean success = addEntryInputStream(zos,
							e.getName(), zipFile.getInputStream(e));
					;
					if (!success) {
						duplicateEntries.add(e.getName());
					}
				}
			} catch (final IOException e1) {
				throw new RuntimeException("Error while merging entry "
						+ e.getName() + " from zip file " + zipFile.getName(),
						e1);
			}
		}
		return duplicateEntries;
	}

	private static boolean addEntryInputStream(ZipOutputStream zos,
			String entryName, InputStream inputStream) {
		final ZipEntry zipEntry = new ZipEntry(entryName);
		try {
			zos.putNextEntry(zipEntry);
		} catch (final ZipException e) {

			// Ignore duplicate entry - no overwriting
			return false;
		} catch (final IOException e) {
			throw new RuntimeException("Error while adding zip entry "
					+ zipEntry, e);
		}
		final int buffer = 2048;
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(
				inputStream, buffer);
		int count;
		try {
			final byte data[] = new byte[buffer];
			while ((count = bufferedInputStream.read(data, 0, buffer)) != -1) {
				zos.write(data, 0, count);
			}
			bufferedInputStream.close();
			inputStream.close();
			zos.closeEntry();
			return true;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a zip entry into the provided <code>ZipOutputStream</code>. The zip
	 * entry is the part of <code>filePathToZip</code> truncated with the
	 * <code>baseFolderPath</code>.
	 * <p>
	 * So a file or folder <code>c:\my\base\folder\my\file\to\zip.txt</code>
	 * will be added in archive using <code>my/file/to/zip.txt</code> entry.
	 */
	public static void addZipEntry(ZipOutputStream zos, File fileToZip,
			File baseFolder) {
		if (!baseFolder.isDirectory()) {
			throw new IllegalArgumentException(baseFolder.getPath()
					+ " is not a directory.");
		}

		if (fileToZip.isDirectory()) {
			final File[] files = fileToZip.listFiles();
			for (final File file : files) {
				addZipEntry(zos, file, baseFolder);
			}
		} else {
			final String filePathToZip;
			final int start;
			try {
				filePathToZip = fileToZip.getCanonicalPath();
				start = baseFolder.getCanonicalPath().length() + 1;
			} catch (final IOException e1) {
				throw new IllegalStateException(e1);
			}

			final int end = filePathToZip.length();
			String entryName = filePathToZip.substring(start, end);
			entryName = entryName.replace(File.separatorChar, '/');
			final FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(filePathToZip);
			} catch (final FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
			addEntryInputStream(zos, entryName, inputStream);
			JkUtilsIO.closeQuietly(inputStream);
		}
	}

	/**
	 * Add a zip entry into the provided <code>ZipOutputStream</code>.
	 */
	public static void addZipEntry(ZipOutputStream zos, File fileToZip,
			String enrtyName) {
		final FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(fileToZip);
		} catch (final FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
		addEntryInputStream(zos, enrtyName, inputStream);
	}

	/**
	 * Same as constructor of {@link ZipFile} but throwing only unchecked exceptions.
	 */
	public static ZipFile newZipFile(File file) {
		try {
			return new ZipFile(file);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Same as {@link ZipFile#close()} but throwing only unchecked exceptions.
	 */
	public static void closeQuietly(ZipFile ... zipFiles) {
		for (final ZipFile zipFile : zipFiles) {
			try {
				zipFile.close();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Copies the content of an url in a cache file. The cached file path will be [jerkar user dir]/cache/url-contents/[last segment of the url (after last '/')].
	 * If the file already exist than the content of the url is not copied and the file is directly returned.
	 */
	public static File copyUrlContentToCacheFile(URL url, PrintStream report, File cacheDir) {
		final String name = JkUtilsString.substringAfterLast(url.getPath(), "/");
		final File result = new File(cacheDir, name);
		if (result.exists()) {
			if (report != null) {
				report.println("Url " + url.toExternalForm() + " transformed to file by reading existing cached file " + result.getAbsolutePath());
			}
			return result;
		}
		JkUtilsFile.createFileIfNotExist(result);
		if (report != null) {
			report.println("Url " + url.toExternalForm() + " transformed to file by creating file " + result.getAbsolutePath());
		}
		copyUrlToFile(url, result);
		return result;
	}

	/**
	 * Copies the content of the given url to the specified file.
	 */
	public static void copyUrlToFile(URL url, File file) {
		final InputStream inputStream;
		final FileOutputStream fileOutputStream;
		try {
			inputStream = url.openStream();
			fileOutputStream = new FileOutputStream(file);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		copy(inputStream, fileOutputStream);
		closeQuietly(inputStream);
		closeQuietly(fileOutputStream);
	}

	/**
	 * Copies the content of the given input stream to a specified output stream.
	 */
	public static void copy(InputStream in, OutputStream out) {
		final byte[] buf = new byte[1024];
		int len;
		try {
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Serializes a given Java object to the specified file.
	 */
	public static void serialize(Object object, File file) {
		try {
			serialize(object, new FileOutputStream(file));
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException("File must exist.", e);
		}
	}

	/**
	 * Serializes a given Java object to the specified output stream.
	 */
	public static void serialize(Object object, OutputStream outputStream) {
		try {
			final OutputStream buffer = new BufferedOutputStream(outputStream);
			final ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(object);
			} finally {
				output.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deserializes the content of the specified file to a Java object.
	 */
	public static Object deserialize(File file) {
		try {
			return deserialize(new FileInputStream(file));
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Deserializes the content of the specified input stream to a Java object.
	 */
	public static Object deserialize(InputStream inputStream) {
		return deserialize(inputStream, JkUtilsIO.class.getClassLoader());
	}

	/**
	 * Deserialises the content of a given input file to a Java object loaded in the specified classloader.
	 */
	public static Object deserialize(InputStream inputStream, final ClassLoader classLoader) {
		final InputStream buffer = new BufferedInputStream(inputStream);
		ObjectInput input;
		try {
			input = new ObjectInputStream(buffer) {

				@Override
				protected Class<?> resolveClass(ObjectStreamClass desc)
						throws IOException, ClassNotFoundException {

					final String name = desc.getName();
					try {
						return Class.forName(name, false, classLoader);
					} catch (final ClassNotFoundException ex) {
						final Class<?> cl = primClasses.get(name);
						if (cl != null) {
							return cl;
						} else {
							throw ex;
						}
					}

				}

			};
		} catch (final IOException e) {
			closeQuietly(buffer);
			throw new RuntimeException(e);
		}
		try {
			return input.readObject();
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			closeQuietly(input);
		}
	}

	/**
	 * Serializes an object from the current classloader and unserializes it in the specified classloader.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cloneBySerialization(Object objectToClone, ClassLoader targetClassLoader) {
		final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		serialize(objectToClone, arrayOutputStream);
		final byte[] bytes = arrayOutputStream.toByteArray();
		final ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
		return (T) deserialize(bin, targetClassLoader);
	}


	/**
	 * Returns a thread that write each data read from the specified input
	 * stream to the specified output stream.
	 */
	public static StreamGobbler newStreamGobbler(InputStream is, OutputStream os) {
		return new StreamGobbler(is, os);
	}

	/**
	 * Runs a thread copying all data from a stream to a specified writer. The
	 * thread is started when the instance is created. You have to call
	 * {@link #stop()} to stop the thread.
	 */
	public static final class StreamGobbler {

		private final InnerRunnable innerRunnable;

		private StreamGobbler(InputStream is, OutputStream os) {
			this.innerRunnable = new InnerRunnable(is, os);
			new Thread(innerRunnable).start();
		}

		/**
		 * Stop the gobbling, meaning stop the thread.
		 */
		public StreamGobbler stop() {
			this.innerRunnable.stop.set(true);
			return this;
		}

		public static class InnerRunnable implements Runnable {

			private final InputStream in;

			private final OutputStream out;

			private final AtomicBoolean stop = new AtomicBoolean(false);

			private InnerRunnable(InputStream is, OutputStream os) {
				this.in = is;
				this.out = os;
			}

			@Override
			public void run() {
				try {
					final InputStreamReader isr = new InputStreamReader(in);
					final BufferedReader br = new BufferedReader(isr);
					String line = null;
					while (!stop.get() && (line = br.readLine()) != null) {
						final byte[] bytes = line.getBytes();
						out.write(bytes, 0, bytes.length);
						out.write('\n');
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}

		}

	}

	/* table mapping primitive type names to corresponding class objects */
	private static final HashMap<String, Class<?>> primClasses = new HashMap<String, Class<?>>(8, 1.0F);

	static {
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
	}



}