package org.jake.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Manifest;

import org.jake.JakeDir;
import org.jake.JakeDirSet;
import org.jake.utils.JakeUtilsIO;

/**
 * Helper class to read and write Manifest from and to file.
 * 
 * @author Jerome Angibaud
 */
public class JakeManifestIO {

	public static final String PATH = "META-INF/MANIFEST.MF";

	public static Manifest read(File file) {
		final Manifest manifest = new Manifest();
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			manifest.read(is);
			return manifest;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			JakeUtilsIO.closeQuietly(is);
		}
	}

	public static Manifest readMetaInfManifest(JakeDirSet jakeDirSet) {
		for (final JakeDir dir : jakeDirSet.jakeDirs()) {
			final File candidate = dir.file(PATH);
			if (candidate.exists()) {
				return read(candidate);
			}
		}
		throw new IllegalArgumentException("No " + PATH + " found in " + jakeDirSet);
	}

	public static void writeTo(Manifest manifest, File file) {
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(file);
			manifest.write(outputStream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			JakeUtilsIO.closeQuietly(outputStream);
		}
	}

	public static void writeToStandardlocation(Manifest manifest, File baseDir) {
		writeTo(manifest, JakeDir.of(baseDir).file(PATH));
	}




}
