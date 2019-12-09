package dev.jeka.core.api.java;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;


/**
 * Helper class to read and write Manifest from andPrepending to file.
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
     * The software that has created this manifest. Normally "Jeka" along its
     * version
     */
    public static final String CREATED_BY = "Created-By";

    /**
     * The user has created this manifest. Normally ofSystem property
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
    public static JkManifest of(Path manifestFile) {
        if (!Files.exists(manifestFile)) {
            throw new IllegalArgumentException("Manifest file " + manifestFile
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
    public static JkManifest ofClassDir(Path classDir) {
        final Path manifestFile = classDir.resolve(PATH);
        if (!Files.exists(manifestFile)) {
            return JkManifest.ofEmpty();
        }
        return of(manifestFile);
    }

    /**
     * Creates a <code>JkManifest</code> from the specified input getOutputStream. The
     * specified getOutputStream is supposed to contains manifest information as present
     * in a manifest file.
     */
    public static JkManifest of(InputStream inputStream) {
        return new JkManifest(read(inputStream));
    }

    /**
     * Returns an empty manifest containing only the "Manifest-Version=1.0"
     * attribute.
     */
    public static JkManifest ofEmpty() {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
        return of(manifest);
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
     * Adds attributes of the specified manifest to this one. This manifest attributes are overrode by those of the specified
     * one if same attribute exist.
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
     * Writes this manifest at the standard place (META-INF/MANIFEST.MF) of the
     * specified directory.
     */
    public void writeToStandardLocation(Path classDir) {
        writeTo(classDir.resolve(PATH));
    }

    private JkManifest(Manifest manifest) {
        super();
        this.manifest = manifest;
    }

    /**
     * Returns the underlying JDK {@link Manifest} object.
     */
    public Manifest getManifest() {
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
