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

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsZip;


/**
 * Helper class to read and write Manifest from and to file.
 *
 * @author Jerome Angibaud
 */
public final class JkManifest {

    /**
     * The path where generally belongs all manifest past (relative to archive
     * asScopedDependency)
     */
    public static final String PATH = "META-INF/MANIFEST.MF";

    /**
     * The JDK version who was running while bulding this manifest.
     */
    public static final String BUILD_JDK = "Build-Jdk";

    /**
     * The software that has created this manifest. Normally "Jerkar" along its
     * version
     */
    public static final String CREATED_BY = "Created-By";

    /**
     * The user has created this manifest. Normally system property
     * <code>user.name</code>
     */
    private static final String BUILT_BY = "Built-By";

    private final Manifest manifest;

    /**
     * Creates a JkManifest from the specified {@link Manifest} object.
     */
    public static JkManifest of(Manifest manifest) {
        return new JkManifest(manifest);
    }

    /**
     * Creates a <code>JkManifest</code> from the specified file. The file is
     * supposed to be a manifest file. If the manifest file does not exist, an
     * {@link IllegalArgumentException} is thrown.
     */
    public static JkManifest of(File manifestFile) {
        if (!manifestFile.exists()) {
            throw new IllegalArgumentException("Manifest file " + manifestFile.getPath()
            + " not found.");
        }
        return new JkManifest(read(manifestFile));
    }

    /**
     * Creates a <code>JkManifest</code> from the specified class dir. This
     * method looks at the META-INF/MANIFEST.MF file inside the specified
     * directory to create the returned manifest. If no such file is found, an
     * empty manifest is returned.
     */
    public static JkManifest ofClassDir(File classDir) {
        final File manifestFile = new File(classDir, PATH);
        if (!manifestFile.exists()) {
            return JkManifest.empty();
        }
        return of(manifestFile);
    }




    /**
     * Creates a <code>JkManifest</code> from the specified input stream. The
     * specified stream is supposed to contains manifest information as present
     * in a manifest file.
     */
    public static JkManifest of(InputStream inputStream) {
        return new JkManifest(read(inputStream));
    }

    /**
     * Returns the Manifest read from the specified archive. The manifest is
     * expected to be found at META-INF/MANIFEST.MF. Returns <code>null</code>
     * if no manifest found.
     */
    public static JkManifest ofArchive(File archive) {
        final InputStream inputStream = JkUtilsZip.readZipEntryOrNull(archive, PATH);
        if (inputStream == null) {
            return null;
        }
        try {
            return JkManifest.of(inputStream);
        } finally {
            JkUtilsIO.closeQuietly(inputStream);
        }
    }

    /**
     * Returns an empty manifest containing only the "Manifest-Version=1.0"
     * attribute.
     */
    public static JkManifest empty() {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
        return of(manifest);
    }

    /**
     * Add the specified attributes in the "main" attributes section.
     * This method return this object.
     */
    public JkManifest addMainAttribute(Name key, String value) {
        this.manifest.getMainAttributes().putValue(key.toString(), value);
        return this;
    }

    /**
     * Add the main class entry by auto-detecting the class holding the main method.
     */
    public JkManifest addAutodetectMain(File classDir) {
        final String mainClassName = JkClassLoader.findMainClass(classDir);
        if (mainClassName != null) {
            this.addMainClass(mainClassName);
        } else {
            throw new IllegalStateException("No class with main method found.");
        }
        return this;
    }

    /**
     * @see #addMainAttribute(Name, String)
     */
    public JkManifest addMainAttribute(String key, String value) {
        this.manifest.getMainAttributes().putValue(key, value);
        return this;
    }

    /**
     * Add the 'Main-Class' attribute to this manifest.
     * This method returns this object.
     */
    public JkManifest addMainClass(String value) {
        return addMainAttribute(Name.MAIN_CLASS, value);
    }

    /**
     * Fills the manifest with contextual infoString : {@link #CREATED_BY},
     * {@link #BUILT_BY} and {@link #BUILD_JDK}
     */
    public JkManifest addContextualInfo() {
        return addMainAttribute(CREATED_BY, "Jerkar").addMainAttribute(BUILT_BY,
                System.getProperty("user.name")).addMainAttribute(BUILD_JDK,
                        System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
    }

    /**
     * Creates a manifest identical to this one but adding attributes of an
     * other one. Attributes of this one are overrided by those of the specified
     * manifest if same attribute exist.
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

    /**
     * Returns the value of the main attribute having the specified name.
     */
    public String mainAttribute(String key) {
        return this.manifest().getMainAttributes().getValue(key);
    }

    /**
     * Returns the value of the main attribute having the specified name.
     */
    public String mainAttribute(Name name) {
        return this.manifest().getMainAttributes().getValue(name);
    }

    private static Manifest read(File file) {
        JkUtilsAssert.isTrue(file.exists(), JkUtilsFile.canonicalPath(file) + " not found.");
        JkUtilsAssert.isTrue(file.isFile(), JkUtilsFile.canonicalPath(file) + " is directory : need file.");
        final Manifest manifest = new Manifest();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            manifest.read(is);
            return manifest;
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
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

    /**
     * Writes this manifest to the specified file.
     */
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

    /**
     * Writes this manifest at the standard place (META-INF/MANIFEST.MF) of the
     * specified directory.
     */
    public void writeToStandardLocation(File classDir) {
        writeTo(new File(classDir, PATH));
    }

    private JkManifest(Manifest manifest) {
        super();
        this.manifest = manifest;
    }

    /**
     * Returns the underlying JDK {@link Manifest} object.
     */
    public Manifest manifest() {
        return manifest;
    }

    /**
     * Returns <code>true</code> if this manifest has no entry or has only
     * "Manifest-Version" entry.
     */
    public boolean isEmpty() {
        final Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes.size() > 1) {
            return false;
        }
        if (mainAttributes.size() == 1
                && !mainAttributes.containsKey(Attributes.Name.MANIFEST_VERSION)) {
            return false;
        }
        return manifest.getEntries().size() == 0;
    }

}
