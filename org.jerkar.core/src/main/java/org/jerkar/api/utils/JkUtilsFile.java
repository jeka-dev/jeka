package org.jerkar.api.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for dealing with files.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsFile {

    /**
     * Returns the content ofMany the specified property file as a
     * {@link Properties} object.
     */
    public static Properties readPropertyFile(File propertyfile) {
        return readPropertyFile(propertyfile.toPath());
    }

    /**
     * Returns the content ofMany the specified property file as a
     * {@link Properties} object.
     */
    public static Properties readPropertyFile(Path propertyfile) {
        final Properties props = new Properties();
        try (InputStream fileInputStream = Files.newInputStream(propertyfile)){
            props.load(fileInputStream);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        return props;
    }

    /**
     * Returns the content ofMany the specified property file as a {@link Map}
     * object.
     */
    public static Map<String, String> readPropertyFileAsMap(File propertyfile) {
        final Properties properties = readPropertyFile(propertyfile);
        return JkUtilsIterable.propertiesToMap(properties);
    }

    public static Map<String, String> readPropertyFileAsMap(Path propertyfile) {
        final Properties properties = readPropertyFile(propertyfile);
        return JkUtilsIterable.propertiesToMap(properties);
    }

    /**
     * Get the url to the specified file.
     */
    public static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
