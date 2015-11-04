package org.jerkar.api.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jerkar.api.utils.JkUtilsThrowable;

/**
 * Wrapper around {@link ZipFile} to avoid checked exceptions.
 */
public class JkZipFile implements Closeable {

	private final ZipFile zipFile;

	private JkZipFile(ZipFile zipFile) {
		super();
		this.zipFile = zipFile;
	}

	/**
	 * @see ZipFile#ZipFile(File)
	 */
	public static JkZipFile of(File file) {
		try {
			return new JkZipFile(new ZipFile(file));
		} catch (final Exception e) {
			throw JkUtilsThrowable.unchecked(e);
		}
	}

	/**
	 * Returns an input stream for reading the content of the specified entry name. If there is no such enntry name, an exception is thrown.
	 */
	public InputStream inputStream(String entryName) {
		try {
			final ZipEntry entry = zipFile.getEntry(entryName);
			if (entry == null) {
				throw new IllegalArgumentException("No entry " + entryName + " found in " + zipFile.getName());
			}
			return zipFile.getInputStream(entry);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close()  {
		try {
			this.zipFile.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}




}
