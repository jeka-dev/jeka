package org.jerkar.api.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;

/**
 * Helper class to read and write Manifest from and to file.
 * 
 * @author Jerome Angibaud
 */
public final class JkManifest {

	public static final String PATH = "META-INF/MANIFEST.MF";

	private final Manifest manifest;

	public static JkManifest of(Manifest manifest) {
		return new JkManifest(manifest);
	}

	public final JkManifest of(JkFileTreeSet fileTrees) {
		return of(readMetaInfManifest(fileTrees));
	}

	public static JkManifest of(File manifestFile) {
		return new JkManifest(read(manifestFile));
	}

	public static JkManifest empty() {
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
		return of(manifest);
	}

	public JkManifest addMainAttribute(Name key, String value) {
		this.manifest.getMainAttributes().putValue(key.toString(), value);
		return this;
	}

	public JkManifest addMainAttribute(String key, String value) {
		this.manifest.getMainAttributes().putValue(key, value);
		return this;
	}

	public JkManifest addMainClass(String value) {
		return addMainAttribute(Name.MAIN_CLASS, value);
	}

	private static Manifest read(File file) {
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

	private static Manifest readMetaInfManifest(JkFileTreeSet jkFileTreeSet) {
		for (final JkFileTree dir : jkFileTreeSet.fileTrees()) {
			final File candidate = dir.file(PATH);
			if (candidate.exists()) {
				return read(candidate);
			}
		}
		throw new IllegalArgumentException("No " + PATH + " found in " + jkFileTreeSet);
	}

	public void writeTo(File file) {
		JkUtilsFile.createFileIfNotExist(file);
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

	public void writeToStandardLocation(File classDir) {
		writeTo(new File(classDir, PATH));
	}

	private JkManifest(Manifest manifest) {
		super();
		this.manifest = manifest;
	}

	public Manifest manifest() {
		return manifest;
	}




}
