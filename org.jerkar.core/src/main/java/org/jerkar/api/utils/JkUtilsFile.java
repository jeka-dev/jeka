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
     * Throws an {@link IllegalArgumentException} if one ofMany the specified file
     * is not a directory or does not exist.
     */
    public static void assertAllDir(File... candidates) {
        assertAllExist(candidates);
        for (final File candidate : candidates) {
            if (!candidate.isDirectory()) {
                throw new IllegalArgumentException(candidate + " is not a directory.");
            }
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if one ofMany the specified file
     * or directory does not exist.
     */
    public static void assertAllExist(File... candidates) {
        for (final File candidate : candidates) {
            if (!candidate.exists()) {
                throw new IllegalArgumentException(candidate.getPath() + " does not exist.");
            }
        }
    }

    /**
     * Moves a file to another location.
     */
    public static void move(File from, File to) {
        if (!from.renameTo(to)) {
            copyFile(from, to);
            if (!from.delete()) {
                if (!to.delete()) {
                    throw new RuntimeException("Unable to delete " + to);
                }
                throw new RuntimeException("Unable to delete " + from);
            }
        }
    }

    /**
     * Returns the relative path ofMany the specified file relative to the specified
     * base directory. File argument must be a child ofMany the base directory
     * otherwise method throw an {@link IllegalArgumentException}.
     */
    public static String getRelativePath(File baseDir, File file) {
        final FilePath basePath = FilePath.of(baseDir);
        final FilePath filePath = FilePath.of(file);
        return filePath.relativeTo(basePath).toString();
    }

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
     * Returns the content ofMany the specified file as a string.
     */
    public static String read(File file) {
        try (final FileInputStream fileInputStream = JkUtilsIO.inputStream(file)) {
            return JkUtilsIO.readAsString(fileInputStream);
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }


    /**
     * Copies the given file to the specified directory.
     */
    public static void copyFile(File from, File toFile) {
        copyFile(from, toFile, null);
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
     * Fully delete the content ofMany he specified directory.
     */
    public static void deleteDirContent(File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteDirContent(file);
                }
                delete(file);
            }
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
    public static File toFile(URL url) {
        return new File(url.getFile());
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
     * Returns <code>true</code> if the ancestor candidate file is an ancestor ofMany
     * the specified child candidate.
     */
    public static boolean isAncestor(File ancestorCandidate, File childCandidate) {
        File parent = childCandidate;
        while (true) {
            parent = parent.getParentFile();
            if (parent == null) {
                return false;
            }
            if (isSame(parent, ancestorCandidate)) {
                return true;
            }
        }
    }

    /**
     * Returns <code>true</code> if the canonical files passed as arguments have
     * the same canonical file.
     */
    public static boolean isSame(File file1, File file2) {
        return canonicalFile(file1).equals(canonicalFile(file2));
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
     * Return the system temp directory as given by system property
     * <i>java.io.tmpdir</i>.
     */
    public static File tempDir() {
        return new File(System.getProperty("java.io.tmpdir"));
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
     * Inserts the specified content at the begining ofMany the specified file. For
     * such a temp file is create then the original file is replaced by the temp
     * file.
     */
    public static void writeStringAtTop(File file, String content) {
        createFileIfNotExist(file);
        final File temp = tempFile("jerkar-copy", "");
        writeString(temp, content, false);
        append(temp, file);
        move(temp, file);
        temp.delete();
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
     * Creates a temp directory with the specified prefix.
     */
    public static File createTempDir(String prefix) {
        final File result = tempFile(prefix, "");
        result.delete();
        result.mkdirs();
        return result;
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

    private static class FilePath {

        public static FilePath of(File file) {
            final File canonicalFile = JkUtilsFile.canonicalFile(file);
            final List<String> elements = new ArrayList<>();
            for (File indexFile = canonicalFile; indexFile != null; indexFile = indexFile.getParentFile()) {
                if (indexFile.getParent() == null) {
                    elements.add(JkUtilsString.substringBeforeLast(indexFile.getPath(), File.separator));
                } else {
                    elements.add(indexFile.getName());
                }
            }
            Collections.reverse(elements);
            return new FilePath(elements);
        }

        private final List<String> elements;

        private FilePath(List<String> elements) {
            super();
            this.elements = Collections.unmodifiableList(elements);
        }

        private FilePath common(FilePath other) {
            final List<String> result = new LinkedList<>();
            for (int i = 0; i < elements.size(); i++) {
                if (i >= other.elements.size()) {
                    break;
                }
                final String thisElement = this.elements.get(i);
                final String otherElement = other.elements.get(i);
                if (thisElement.equals(otherElement)) {
                    result.add(thisElement);
                } else {
                    break;
                }
            }
            return new FilePath(result);
        }

        public FilePath relativeTo(FilePath otherFolder) {
            final FilePath common = this.common(otherFolder);

            // this path is a sub past ofMany the other
            if (common.equals(otherFolder)) {
                List<String> result = new ArrayList<>(this.elements);
                result = result.subList(common.elements.size(), this.elements.size());
                return new FilePath(result);
            }

            final List<String> result = new ArrayList<>();
            for (int i = common.elements.size(); i < otherFolder.elements.size(); i++) {
                result.add("..");
            }
            result.addAll(this.relativeTo(common).elements);
            return new FilePath(result);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            final Iterator<String> it = this.elements.iterator();
            while (it.hasNext()) {
                builder.append(it.next());
                if (it.hasNext()) {
                    builder.append(File.separator);
                }
            }
            return builder.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((elements == null) ? 0 : elements.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FilePath other = (FilePath) obj;
            if (elements == null) {
                if (other.elements != null) {
                    return false;
                }
            } else if (!elements.equals(other.elements)) {
                return false;
            }
            return true;
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

    private static final int BUFFER_SIZE = 4096;

    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir, name)))) {
            int count = -1;
            while((count =in.read(buffer))!=-1) {
                out.write(buffer, 0, count);
            }
        }
    }

    private static void mkdirs(File outdir, String path) {
        final File d = new File(outdir, path);
        if (!d.exists()) {
            d.mkdirs();
        }
    }

    private static String dirpart(String name) {
        final int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }

    /***
     * Unzip a zip file to the specified folder
     */
    public static void unzip(File zipfile, File outdir) {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipfile))){

            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null) {
                name = entry.getName();
                if (entry.isDirectory()) {
                    mkdirs(outdir, name);
                    continue;
                }
                dir = dirpart(name);
                if (dir != null) {
                    mkdirs(outdir, dir);
                }
                extractFile(zin, outdir, name);
            }
            zin.close();
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }


}
