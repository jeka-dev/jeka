package org.jerkar.api.utils;

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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for dealing with files.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsFile {

    /**
     * Throws an {@link IllegalArgumentException} if one of the specified file
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
     * Throws an {@link IllegalArgumentException} if one of the specified file
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
     * Returns the relative path of the specified file relative to the specified
     * base directory. File argument must be a child of the base directory
     * otherwise method throw an {@link IllegalArgumentException}.
     */
    public static String getRelativePath(File baseDir, File file) {
        final FilePath basePath = FilePath.of(baseDir);
        final FilePath filePath = FilePath.of(file);
        return filePath.relativeTo(basePath).toString();
    }

    /**
     * Same as
     * {@link #copyDirContent(File, File, FileFilter, boolean, PrintStream)}
     * without stream report and file filter.
     */
    public static int copyDirContent(File source, File targetDir, boolean copyEmptyDir) {
        return copyDirContent(source, targetDir, null, copyEmptyDir, null);
    }

    /**
     * Same as
     * {@link #copyDirContent(File, File, FileFilter, boolean, PrintStream)}
     * without stream report.
     */
    public static int copyDirContent(File source, File targetDir, FileFilter filter, boolean copyEmptyDir) {
        return copyDirContent(source, targetDir, filter, copyEmptyDir, null);
    }

    /**
     * Copies the source directory content to the target directory.
     *
     * @param source
     *            The directory we want copy the content from.
     * @param targetDir
     *            The directory where will be copied the content
     * @param filter
     *            Filter to decide which file should be copied or not. If you
     *            want to copy everything, use {@link FileFilter} that always
     *            return <code>true</code>.
     * @param copyEmptyDir
     *            specify if the empty dirs should be copied as well.
     * @param reportStream
     *            The scream where a copy status can be written. If
     *            <code>null</code>, no status is written.
     * @return The file copied count.
     */
    public static int copyDirContent(File source, File targetDir, FileFilter filter, boolean copyEmptyDir,
            PrintStream reportStream) {
        return copyDirContentReplacingTokens(source, targetDir, filter, copyEmptyDir, reportStream, null);
    }

    /**
     * Same as
     * {@link #copyDirContent(File, File, FileFilter, boolean, PrintStream)} but
     * also replacing all token as '${key}' by their respecting value. The
     * replacement is done only if the specified tokenValues map contains the
     * according key.
     *
     * @param tokenValues
     *            a map for replacing token key by value
     */
    public static int copyDirContentReplacingTokens(File fromDir, File toDir, FileFilter filterArg,
            boolean copyEmptyDir, PrintStream reportStream, Map<String, String> tokenValues) {
        final FileFilter filter = JkUtilsObject.firstNonNull(filterArg, JkFileFilters.acceptAll());
        assertAllDir(fromDir);
        if (fromDir.equals(toDir)) {
            throw new IllegalArgumentException(
                    "Base and destination directory can't be the same : " + fromDir.getPath());
        }
        if (isAncestor(fromDir, toDir) && filter.accept(toDir)) {
            throw new IllegalArgumentException("Base filtered directory " + fromDir.getPath() + ":(" + filter
                    + ") cannot contain destination directory " + toDir.getPath()
                    + ". Narrow filter or change the target directory.");
        }
        if (toDir.isFile()) {
            throw new IllegalArgumentException(toDir.getPath() + " is file. Should be directory");
        }

        if (reportStream != null) {
            reportStream.append("Coping content of " + fromDir.getPath());
        }
        final File[] children = fromDir.listFiles();
        int count = 0;
        for (final File child : children) {
            if (child.isFile()) {
                if (filter.accept(child)) {
                    final File targetFile = new File(toDir, child.getName());
                    if (tokenValues == null || tokenValues.isEmpty()) {
                        copyFile(child, targetFile, reportStream);
                    } else {
                        final File toFile = new File(toDir, targetFile.getName());
                        copyFileReplacingTokens(child, toFile, tokenValues, reportStream);
                    }

                    count++;
                }
            } else {
                final File subdir = new File(toDir, child.getName());
                if (filter.accept(child) && copyEmptyDir) {
                    subdir.mkdirs();
                }
                final int subCount = copyDirContentReplacingTokens(child, subdir, filter, copyEmptyDir, reportStream,
                        tokenValues);
                count = count + subCount;
            }

        }
        return count;
    }

    /**
     * Returns the content of the specified property file as a
     * {@link Properties} object.
     */
    public static Properties readPropertyFile(File propertyfile) {
        final Properties props = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(propertyfile);
            props.load(fileInputStream);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            JkUtilsIO.closeQuietly(fileInputStream);
        }
        return props;
    }

    /**
     * Returns the content of the specified property file as a {@link Map}
     * object.
     */
    public static Map<String, String> readPropertyFileAsMap(File propertyfile) {
        final Properties properties = readPropertyFile(propertyfile);
        return JkUtilsIterable.propertiesToMap(properties);
    }

    /**
     * Returns the content of the specified file as a string.
     */
    public static String read(File file) {
        final FileInputStream fileInputStream = JkUtilsIO.inputStream(file);
        final String result = JkUtilsIO.readAsString(fileInputStream);
        JkUtilsIO.closeQuietly(fileInputStream);
        return result;
    }

    /**
     * Returns the content of the specified file as a list of string.
     */
    public static List<String> readLines(File file) {
        final FileInputStream fileInputStream = JkUtilsIO.inputStream(file);
        final List<String> result = JkUtilsIO.readAsLines(fileInputStream);
        JkUtilsIO.closeQuietly(fileInputStream);
        return result;
    }

    /**
     * Copies the given file to the specified directory.
     */
    public static void copyFileToDir(File from, File toDir) {
        final File to = new File(toDir, from.getName());
        copyFile(from, to, null);
    }

    /**
     * Copies the given file to the specified directory, writting status on the
     * provided reportStream.
     */
    public static void copyFileToDir(File from, File toDir, PrintStream reportStream) {
        final File to = new File(toDir, from.getName());
        copyFile(from, to, reportStream);
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
        try {
            final InputStream in = new FileInputStream(from);
            if (!toFile.getParentFile().exists()) {
                toFile.getParentFile().mkdirs();
            }
            if (!toFile.exists()) {
                toFile.createNewFile();
            }
            final OutputStream out = new FileOutputStream(toFile);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (final IOException e) {
            throw new RuntimeException(
                    "IO exception occured while copying file " + from.getPath() + " to " + toFile.getPath(), e);
        }

    }

    /**
     * Fully delete the content of he specified directory.
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
     * Get the url from the specified file.
     */
    public static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Get the file from the specified url.
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
     * Returns <code>true</code> if the ancestorCandidate file is an ancestor of
     * the specified childCandidate.
     */
    public static boolean isAncestor(File ancestorCandidate, File childCandidtate) {
        File parent = childCandidtate;
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
     * A 'checked exception free' version of {@link File#getCanonicalPath()}.
     */
    public static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A 'checked exception free' version of {@link File#getCanonicalFile()}.
     */
    public static File canonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (final IOException e) {
            throw new RuntimeException("Erreur while getting canonical file of " + file, e);
        }
    }

    /**
     * Returns all files contained recursively in the specified directory.
     */
    public static List<File> filesOf(File dir, boolean includeFolder) {
        return filesOf(dir, JkFileFilters.acceptAll(), includeFolder);
    }

    /**
     * Returns all files contained recursively in the specified directory.
     */
    public static List<File> filesOf(File dir, FileFilter fileFilter, boolean includeFolders) {
        assertAllDir(dir);
        final List<File> result = new LinkedList<File>();
        for (final File file : dir.listFiles()) {
            if (file.isFile() && !fileFilter.accept(file)) {
                continue;
            }
            if (file.isDirectory()) {
                if (includeFolders && fileFilter.accept(file)) {
                    result.add(file);
                }
                result.addAll(filesOf(file, fileFilter, includeFolders));
            } else {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Returns <code>true</code> if the specified directory is empty.
     */
    public static boolean isEmpty(File dir, boolean countFolders) {
        return count(dir, JkFileFilters.acceptAll(), countFolders) == 0;
    }

    /**
     * Returns count of files contained recursively in the specified directory.
     * If the dir does not exist then it returns 0.
     */
    public static int count(File dir, FileFilter fileFilter, boolean includeFolders) {
        int result = 0;
        if (!dir.exists()) {
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
     * Return the user directory as given by system property <i>user.home</i>.
     */
    public static File userHome() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Writes the specified content in the the specified file. If append is
     * <code>true</code> the content is written at the end of the file.
     */
    public static void writeString(File file, String content, boolean append) {
        try {
            createFileIfNotExist(file);
            final FileWriter fileWriter = new FileWriter(file, append);
            fileWriter.append(content);
            fileWriter.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts the specified content at the begining of the specified file. For
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
     * Inserts the appender file at the end of the result file.
     */
    public static void append(File result, File appender) {
        final OutputStream out = JkUtilsIO.outputStream(result, true);
        final InputStream in = JkUtilsIO.inputStream(appender);
        JkUtilsIO.copy(in, out);
        JkUtilsIO.closeQuietly(in);
        JkUtilsIO.closeQuietly(out);
    }

    /**
     * Returns the checksum of a specified file. The algorithm may be "SHA-1" or
     * "MD5".
     */
    public static String checksum(File file, String algorithm) {
        final InputStream is = JkUtilsIO.inputStream(file);
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            final byte[] buf = new byte[2048];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            final byte[] bytes = md.digest();
            return JkUtilsString.toHexString(bytes);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        } finally {
            JkUtilsIO.closeQuietly(is);
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
     * Creates a file with the specified name in the system temp folder.
     */
    public static File tempFile(String name) {
        final File folder = new File(System.getProperty("java.io.tmpdir"));
        return createFileIfNotExist(new File(folder, name));
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
     * Deletes the specified dir recursively, throwing a
     * {@link RuntimeException} if the delete fails.
     */
    public static void deleteDir(File dir) {
        deleteDirContent(dir);
        if (!dir.delete()) {
            throw new RuntimeException("Directory " + dir.getAbsolutePath() + " can't be deleted.");
        }
    }

    /**
     * Tries to delete the specified dir recursively.
     */
    public static boolean tryDeleteDir(File dir) {
        deleteDirContent(dir);
        return dir.delete();
    }

    /**
     * Copies the content of the specified in file into the specified out file.
     * While coping token ${key} are replaced by the value found in the
     * specified replacements map.
     *
     * @see #copyDirContentReplacingTokens(File, File, FileFilter, boolean,
     *      PrintStream, Map)
     */
    public static void copyFileWithInterpolation(File in, File out, Map<String, String> replacements) {
        copyFileReplacingTokens(in, out, replacements, null);
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
        final TokenReplacingReader replacingReader = new TokenReplacingReader(from, replacements);
        createFileIfNotExist(toFile);
        final Writer writer;
        try {
            writer = new FileWriter(toFile);
        } catch (final IOException e) {
            JkUtilsIO.closeQuietly(replacingReader);
            throw new RuntimeException(e);
        }
        if (reportStream != null) {
            reportStream.println("Coping and replacing tokens " + replacements + " from file " + from.getAbsolutePath()
            + " to " + toFile.getAbsolutePath());
        }
        final char[] buf = new char[1024];
        int len;
        try {
            while ((len = replacingReader.read(buf)) > 0) {
                writer.write(buf, 0, len);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            JkUtilsIO.closeQuietly(writer);
            JkUtilsIO.closeQuietly(replacingReader);
        }
    }

    /**
     * Copies the content of the specified url to the specified file. While
     * coping token ${key} are replaced by the value found in the specified
     * replacements map.
     *
     * @see #copyFileWithInterpolation(File, File, Map)
     */
    public static void copyUrlReplacingTokens(URL url, File toFile, Map<String, String> replacements,
            PrintStream reportStream) {
        final InputStream is;
        try {
            is = url.openStream();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        copyStreamWithInterpolation(is, toFile, replacements, reportStream);
        JkUtilsIO.closeQuietly(is);
    }

    /**
     * Copies the content of the specified input Stream to the specified file.
     * While coping token ${key} are replaced by the value found in the
     * specified replacements map.
     *
     * @see #copyFileWithInterpolation(File, File, Map)
     */
    public static void copyStreamWithInterpolation(InputStream inputStream, File toFile,
            Map<String, String> replacements, PrintStream reportStream) {
        final TokenReplacingReader replacingReader = new TokenReplacingReader(new InputStreamReader(inputStream),
                replacements);
        if (!toFile.exists()) {
            try {
                toFile.createNewFile();
            } catch (final IOException e) {
                JkUtilsIO.closeQuietly(replacingReader);
                throw new RuntimeException(e);
            }
        }
        final Writer writer;
        try {
            writer = new FileWriter(toFile);
        } catch (final IOException e) {
            JkUtilsIO.closeQuietly(replacingReader);
            throw new RuntimeException(e);
        }
        final char[] buf = new char[1024];
        int len;
        try {
            while ((len = replacingReader.read(buf)) > 0) {
                writer.write(buf, 0, len);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            JkUtilsIO.closeQuietly(writer);
            JkUtilsIO.closeQuietly(replacingReader);
        }
    }



    private static class FilePath {

        public static FilePath of(File file) {
            final List<String> elements = new ArrayList<String>();
            for (File indexFile = file; indexFile != null; indexFile = indexFile.getParentFile()) {
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
            final List<String> result = new LinkedList<String>();
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

            // this path is a sub past of the other
            if (common.equals(otherFolder)) {
                List<String> result = new ArrayList<String>(this.elements);
                result = result.subList(common.elements.size(), this.elements.size());
                return new FilePath(result);
            }

            final List<String> result = new ArrayList<String>();
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
     * @throws IllegalArgumentException
     *             If the specified resource does not exist.
     */
    public static File resourceAsFile(Class<?> clazz, String resourceName) {
        final URL url = clazz.getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("No resource " + resourceName + " found for class " + clazz.getName());
        }
        return fromUrl(url);
    }

}
