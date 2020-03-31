package dev.jeka.core.api.java;


import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;


/**
 * Helper class to read and write Manifest from and to file.
 *
 * @author Jerome Angibaud
 */
public final class JkManifest<T> {

    /**
     * The path where generally belongs all manifest past (relative to archive root)
     */
    public static final String STANDARD_LOCATION = "META-INF/MANIFEST.MF";

    /**
     * The JDK version who was running while building this manifest.
     */
    public static final String BUILD_JDK = "Build-Jdk";

    /**
     * The software that has created this manifest. Normally "Jeka" along its
     * version
     */
    public static final String CREATED_BY = "Created-By";

    /**
     * The user has created this manifest. Normally of property
     * <code>user.name</code>
     */
    private static final String BUILT_BY = "Built-By";

    /**
     * The title of the implementation.
     */
    public static final String IMPLEMENTATION_TITLE = "Implementation-Title";

    /**
     * The version of the implementation.
     */
    public static final String IMPLEMENTATION_VERSION = "Implementation-Version";

    /**
     * The version of the implementation.
     */
    public static final String IMPLEMENTATION_VENDOR = "Implementation-Vendor";

    private Manifest manifest;

    /**
     *  For parent chaining
     */
    public final T _;

    private JkManifest(T _) {
        this._ = _;
        this.manifest = new Manifest();
        this.manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
    }

    /**
     * Creates an empty JkManifest
     */
    public static JkManifest<Void> of() {
        return new JkManifest(null);
    }

    /**
     * Same as {@link #of()} but providing a parent for method chaining
     */
    public static <T> JkManifest<T> of(T parent) {
         return new JkManifest(parent);
    }

    /**
     * Returns the underlying JDK {@link Manifest} object.
     */
    public Manifest getManifest() {
        return manifest;
    }

    /**
     * Sets the underlying {@link Manifest} object. T
     */
    public JkManifest<T> setManifest(Manifest manifest) {
        this.manifest = manifest;
        return this;
    }

    /**
     * Sets the underlying {@link Manifest} object from the content of the specified file.
     */
    public JkManifest<T> setManifestFromFile(Path file) {
        this.manifest = read(file);
        return this;
    }

    /**
     * Sets the underlying {@link Manifest} object from the file present at [specified class dir]/META-INF/MANIFEST.MF
     */
    public JkManifest<T> setManifestFromClassRootDir(Path classDir) {
        final Path manifestFile = classDir.resolve(STANDARD_LOCATION);
        return this.setManifestFromFile(manifestFile);
    }

    /**
     * Creates a <code>JkManifest</code> from the specified input getOutputStream. The
     * specified getOutputStream is supposed to contains manifest information as present
     * in a manifest file.
     */
    public JkManifest<T> setManifestFromInputStream(InputStream inputStream) {
        return this.setManifest(read(inputStream));
    }

    /**
     * Adds the specified attributes in the "main" attributes section.
     * This method return this object.
     */
    public JkManifest<T> addMainAttribute(Name key, String value) {
        this.manifest.getMainAttributes().putValue(key.toString(), value);
        return this;
    }

    /**
     * Adds the main class entry by auto-detecting the class holding the main method.
     */
    public JkManifest<T> addAutodetectMain(Path classDir) {
        ClassLoader classLoader = JkUrlClassLoader.of(classDir).get();
        List<String> classes = JkInternalClasspathScanner.INSTANCE.findClassesHavingMainMethod(classLoader);
        if (!classes.isEmpty()) {
            this.addMainClass(classes.get(0));
        } else {
            throw new IllegalStateException("No class with main method found.");
        }
        return this;
    }

    /**
     * @see #addMainAttribute(Name, String)
     */
    public JkManifest<T> addMainAttribute(String key, String value) {
        this.manifest.getMainAttributes().putValue(key, value);
        return this;
    }

    /**
     * Adds the 'Main-Class' attribute to this manifest.
     * This method returns this object.
     */
    public JkManifest<T> addMainClass(String value) {
        return addMainAttribute(Name.MAIN_CLASS, value);
    }

    /**
     * Fills this manifest with contextual infoString : {@link #CREATED_BY},
     * {@link #BUILT_BY} and {@link #BUILD_JDK}
     */
    public JkManifest<T> addContextualInfo() {
        return addMainAttribute(CREATED_BY, "Jeka").addMainAttribute(BUILT_BY,
                System.getProperty("user.name")).addMainAttribute(BUILD_JDK,
                        System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
    }

    /**
     * Returns the value of the main attribute having the specified name.
     */
    public String getMainAttribute(String key) {
        return this.getManifest().getMainAttributes().getValue(key);
    }

    /**
     * Returns the value of the main attribute having the specified name.
     */
    public String getMainAttribute(Name name) {
        return this.getManifest().getMainAttributes().getValue(name);
    }

    /**
     * Adds attributes of the specified manifest to this one. This manifest attributes are overrode by those of the specified
     * one if same attribute exist.
     */
    public JkManifest<T> merge(Manifest other) {
        final Map<String, Attributes> otherEntryAttributes = other.getEntries();
        for (final String entry : otherEntryAttributes.keySet()) {
            final Attributes otherAttributes = otherEntryAttributes.get(entry);
            final Attributes attributes = this.manifest.getAttributes(entry);
            merge(attributes, otherAttributes);
        }
        merge(this.manifest.getMainAttributes(), other.getMainAttributes());
        return this;
    }

    /**
     * Writes this manifest at the standard place (META-INF/MANIFEST.MF) of the
     * specified directory.
     */
    public void writeToStandardLocation(Path classDir) {
        writeTo(classDir.resolve(STANDARD_LOCATION));
    }

    private static void merge(Attributes attributes, Attributes others) {
        for (final Map.Entry<?, ?> entry : others.entrySet()) {
            attributes.putValue(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    private static Manifest read(Path file) {
        JkUtilsAssert.isTrue(Files.exists(file), file.normalize() + " not found.");
        JkUtilsAssert.isTrue(Files.isRegularFile(file), file.normalize() + " is directory : need file.");
        final Manifest manifest = new Manifest();
        try (InputStream is = Files.newInputStream(file)){
            manifest.read(is);
            return manifest;
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
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

    public void writeTo(Path file) {
        JkUtilsPath.createFileSafely(file);
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            manifest.write(outputStream);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
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
