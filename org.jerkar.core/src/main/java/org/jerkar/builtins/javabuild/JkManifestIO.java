package org.jerkar.builtins.javabuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Manifest;

import org.jerkar.JkFileTree;
import org.jerkar.JkFileTreeSet;
import org.jerkar.utils.JkUtilsIO;

/**
 * Helper class to read and write Manifest from and to file.
 * 
 * @author Jerome Angibaud
 */
public class JkManifestIO {

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
			JkUtilsIO.closeQuietly(is);
		}
	}

	public static Manifest readMetaInfManifest(JkFileTreeSet jkFileTreeSet) {
		for (final JkFileTree dir : jkFileTreeSet.jkFileTrees()) {
			final File candidate = dir.file(PATH);
			if (candidate.exists()) {
				return read(candidate);
			}
		}
		throw new IllegalArgumentException("No " + PATH + " found in " + jkFileTreeSet);
	}

	public static void writeTo(Manifest manifest, File file) {
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(file);
			manifest.write(outputStream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			JkUtilsIO.closeQuietly(outputStream);
		}
	}

	public static void writeToStandardlocation(Manifest manifest, File baseDir) {
		writeTo(manifest, JkFileTree.of(baseDir).file(PATH));
	}




}
