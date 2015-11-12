package org.jerkar.api.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsThrowable;

/**
 * Helper class to read and write Manifest from and to file.
 *
 * @author Jerome Angibaud
 */
public final class JkManifest {

    /**
     * The path where generally belongs all manifest past (relative to archive root)
     */
    public static final String PATH = "META-INF/MANIFEST.MF";

    /**
     * The JDK version who was running while bulding this manifest.
     */
    public static final String BUILD_JDK = "Build-Jdk";

    /**
     * The software that has created this manifest. Normally "Jerkar" along its version
     */
    public static final String CREATED_BY = "Created-By";

    /**
     * The user has created this manifest. Normally system property <code>user.name</code>
     */
    public static final String BUILT_BY = "Built-By";

    public static final String SPECIFICATION_TITLE = "Specification-Title";

    public static final String SPECIFICATION_VERSION = "Specification-Version";

    public static final String SPECIFICATION_VENDOR = "Specification-Vendor";

    public static final String IMPLEMENTATION_TITLE = "Implementation-Title";

    public static final String IMPLEMENTATION_VERSION = "Implementation-Version";

    public static final String IMPLEMENTATION_VENDOR_ID = "Implementation-Vendor-Id";

    public static final String IMPLEMENTATION_URL = "Implementation-URL";

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

    public static JkManifest of(InputStream inputStream) {
	return new JkManifest(read(inputStream));
    }

    /**
     * Returns the Manifest read from the specified archive. The manifest is expected
     * to be found at META-INF/MANIFEST.MF. Returns <code>null</code> if no manifest found.
     */
    public static JkManifest ofArchive(File archive) {
	final InputStream inputStream = JkUtilsIO.readZipEntryOrNull(archive, PATH);
	if (inputStream == null) {
	    return null;
	}
	try {
	    return JkManifest.of(inputStream);
	} finally {
	    JkUtilsIO.closeQuietly(inputStream);
	}
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

    /**
     * Fills the manifest with contextual info : {@link #CREATED_BY}, {@link #BUILT_BY} and {@link #BUILD_JDK}
     */
    public JkManifest addContextualInfo() {
	return addMainAttribute(CREATED_BY, "Jerkar")
		.addMainAttribute(BUILT_BY, System.getProperty("user.name"))
		.addMainAttribute(BUILD_JDK, System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
    }

    /**
     * Creates a manifest identical to this one but adding attributes of an other one.
     * Attributes of this one are overrided by those of the specified manifest if same attribute exist.
     */
    public JkManifest merge(JkManifest other) {
	final Map<String, Attributes> otherEntryAttributes = other.manifest.getEntries();
	for (final String entry : otherEntryAttributes.keySet()) {
	    final Attributes otherAttributes = otherEntryAttributes.get(entry);
	    final Attributes attributes = this.manifest.getAttributes(entry);
	    merge(attributes, otherAttributes);
	}
	merge(this.manifest.getMainAttributes(), other.manifest.getMainAttributes());
	return this;
    }

    private static void merge(Attributes attributes, Attributes others) {
	for (final Object key : others.entrySet()) {
	    attributes.putValue(key.toString(), others.getValue(key.toString()));
	}
    }

    public String mainAttribute(String key) {
	return this.manifest().getMainAttributes().getValue(key);
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

    private static Manifest read(InputStream inputStream) {
	final Manifest manifest = new Manifest();
	try {
	    manifest.read(inputStream);
	    return manifest;
	} catch (final IOException e) {
	    throw JkUtilsThrowable.unchecked(e);
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
