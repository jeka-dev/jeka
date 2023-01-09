package dev.jeka.core.api.java;


import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
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

    private JkManifest() {
        this.manifest = new Manifest();
        this.manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
    }

    /**
     * Creates an empty JkManifest
     */
    public static JkManifest of() {
        return new JkManifest();
    }

    /**
     * Returns the underlying JDK {@link Manifest} object.
     */
    public Manifest getManifest() {
        return manifest;
    }

    /**
     * Set the underlying {@link Manifest} with the specified one.
     */
    public JkManifest set(Manifest manifest) {
        this.manifest = manifest;
        return this;
    }

    /**
     * Loads the manifest file from the specified path and set it as the underlying manifest.
     */
    public JkManifest loadFromFile(Path file) {
        this.manifest = read(file);
        return this;
    }

    public JkManifest loadFromJar(Path jar) {
        try (JkZipTree jarTree = JkZipTree.of(jar)) {
            Path manifestFile = jarTree.get(STANDARD_LOCATION);
            if (!Files.exists(manifestFile)) {
                throw new IllegalArgumentException("File " + jar + " does not contains " + STANDARD_LOCATION + " entry.");
            }
            loadFromFile(manifestFile);
        }
        return this;
    }

    /**
     * Loads the manifest from the specified input stream and set it as the underlying manifest.
     */
    public JkManifest loadFromInputStream(InputStream inputStream) {
        return this.set(read(inputStream));
    }

    /**
     * Loads the manifest from the jar or the base class directory the specified class belongs to.
     */
    public JkManifest loadFromClass(Class<?> clazz) {
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            return loadFromInputStream(clazz.getClassLoader().getResourceAsStream(STANDARD_LOCATION));
        }
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/" + STANDARD_LOCATION;
        try {
            return loadFromInputStream(new URL(manifestPath).openStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Adds the specified attributes in the "main" attributes section.
     * This method return this object.
     */
    public JkManifest addMainAttribute(Name key, String value) {
        this.manifest.getMainAttributes().putValue(key.toString(), value);
        return this;
    }

    /**
     * Adds the main class entry by auto-detecting the class holding the main method.
     */
    public JkManifest addAutodetectMain(Path classDir) {
        ClassLoader classLoader = JkUrlClassLoader.of(classDir).get();
        List<String> classes = JkInternalClasspathScanner.of().findClassesHavingMainMethod(classLoader);
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
    public JkManifest addMainAttribute(String key, String value) {
        this.manifest.getMainAttributes().putValue(key, value);
        return this;
    }

    /**
     * Adds the 'Main-Class' attribute to this manifest.
     * This method returns this object.
     */
    public JkManifest addMainClass(String value) {
        return addMainAttribute(Name.MAIN_CLASS, value);
    }

    /**
     * Fills this manifest with contextual infoString : {@link #CREATED_BY},
     * {@link #BUILT_BY} and {@link #BUILD_JDK}
     */
    public JkManifest addContextualInfo() {
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
     * Adds attributes of the specified manifest to this one. This manifest attributes are overridden by those of the specified
     * one if same attribute exist.
     */
    public JkManifest merge(Manifest other) {
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
        JkUtilsAssert.argument(Files.exists(file), file.normalize() + " not found.");
        JkUtilsAssert.argument(Files.isRegularFile(file), file.normalize() + " is directory : need file.");
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
