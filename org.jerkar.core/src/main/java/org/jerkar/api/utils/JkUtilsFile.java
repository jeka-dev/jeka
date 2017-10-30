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
     * Copies the given file to the specified directory printing a report into
     * the specified report stream.
     */
    public static void copyFile(File from, File toFile, PrintStream reportStream) {
        createFileIfNotExist(toFile);
        if (reportStream != null) {
            reportStream.println("Coping file " + from.getAbsolutePath() + " to " + toFile.getAbsolutePath());
        }
        if (!from.exists()) {
            throw new IllegalArgumentException("File " + from.getPath() + " does not exist.");
        }
        if (from.isDirectory()) {
            throw new IllegalArgumentException(from.getPath() + " is a directory. Should be a file.");
        }
        try (final InputStream in = new FileInputStream(from); final OutputStream out = new FileOutputStream(toFile)){

            if (!toFile.getParentFile().exists()) {
                toFile.getParentFile().mkdirs();
            }
            if (!toFile.exists()) {
                toFile.createNewFile();
            }
            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (final IOException e) {
            throw new RuntimeException(
                    "IO exception occured while copying file " + from.getPath() + " to " + toFile.getPath(), e);
        }

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

    /**
     * Get the file to the specified url.
     */
    public static File fromUrl(URL url) {
        File result;
        try {
            result = new File(url.toURI());
        } catch (final URISyntaxException e) {
            result = new File(url.getPath());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(url + " : " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * A 'checked exception free' version ofMany {@link File#getCanonicalPath()}.
     */
    public static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A 'checked exception free' version ofMany {@link File#getCanonicalFile()}.
     */
    public static File canonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (final IOException e) {
            throw new RuntimeException("Erreur while getting canonical file ofMany " + file, e);
        }
    }

    /**
     * Returns count ofMany files contained recursively in the specified directory.
     * If the dir does not exist then it returns 0.
     */
    public static int count(File dir, FileFilter fileFilter, boolean includeFolders) {
        int result = 0;
        if (!dir.exists()) {
            return 0;
        }
        if (dir.listFiles() == null) {
            return 0;
        }
        for (final File file : dir.listFiles()) {
            if (file.isFile() && !fileFilter.accept(file)) {
                continue;
            }
            if (file.isDirectory()) {
                if (includeFolders) {
                    result++;
                }
                result = result + count(file, fileFilter, includeFolders);
            } else {
                result++;
            }
        }
        return result;
    }

    /**
     * Returns the current working directory.
     */
    public static File workingDir() {
        return JkUtilsFile.canonicalFile(new File("."));
    }

    /**
     * Writes the specified content in the the specified file. If append is
     * <code>true</code> the content is written at the end ofMany the file.
     */
    public static void writeString(File file, String content, boolean append) {
        try {
            createFileIfNotExist(file);
            try (final FileWriter fileWriter = new FileWriter(file, append)) {
                fileWriter.append(content);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts the appender file at the end ofMany the result file.
     */
    public static void append(File result, File appender) {
        try (
                final OutputStream out = JkUtilsIO.outputStream(result, true);
                final InputStream in = JkUtilsIO.inputStream(appender)) {
            JkUtilsIO.copy(in, out);
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }

    }



    /**
     * Same as {@link File#createTempFile(String, String)} but throwing only
     * unchecked exceptions.
     */
    public static File tempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Creates the specified file on the File system if not exist.
     */
    public static File createFileIfNotExist(File file) {
        try {
            if (!file.exists()) {
                if (file.getParent() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            return file;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes the specified file, throwing a {@link RuntimeException} if the
     * delete fails.
     */
    public static void delete(File file) {
        if (!file.delete()) {
            throw new RuntimeException("File " + file.getAbsolutePath() + " can't be deleted.");
        }
    }

    /**
     * Same as {@link #copyFileReplacingTokens(File, File, Map, PrintStream)}
     * but writing the status in the specified reportStream.
     */
    public static void copyFileReplacingTokens(File from, File toFile, Map<String, String> replacements,
            PrintStream reportStream) {
        if (replacements == null || replacements.isEmpty()) {
            copyFile(from, toFile, reportStream);
            return;
        }
        if (!from.exists()) {
            throw new IllegalArgumentException("File " + from.getPath() + " does not exist.");
        }
        if (from.isDirectory()) {
            throw new IllegalArgumentException(from.getPath() + " is a directory. Should be a file.");
        }
        if (reportStream != null) {
            reportStream.println("Coping and replacing tokens " + replacements + " to file " + from.getAbsolutePath()
            + " to " + toFile.getAbsolutePath());
        }
        createFileIfNotExist(toFile);
        try (
                final TokenReplacingReader replacingReader = new TokenReplacingReader(from, replacements);
                final Writer writer = new FileWriter(toFile) ) {
            final char[] buf = new char[1024];
            int len;

            while ((len = replacingReader.read(buf)) > 0) {
                writer.write(buf, 0, len);
            }
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }

    }

    /**
     * Returns a resource as a {@link File}.
     *
     * @throws IllegalArgumentException If the specified resource does not exist.
     */
    public static File resourceAsFile(Class<?> clazz, String resourceName) {
        final URL url = clazz.getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("No resource " + resourceName + " found for class " + clazz.getName());
        }
        return fromUrl(url);
    }






}
