package org.jake.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipOutputStream;

public final class JakeUtilsFile {

	public static void assertDir(File candidate) {
		if (!candidate.exists()) {
			throw new IllegalArgumentException(candidate.getPath()
					+ " does not exist.");
		}
		if (!candidate.isDirectory()) {
			throw new IllegalArgumentException(candidate
					+ " is not a directory.");
		}
	}

	/**
	 * Returns the relative path of the specified file relative to the specified base directory.
	 * File argument must be a child of the base directory otherwise method throw an {@link IllegalArgumentException}.
	 */
	public static String getRelativePath(File baseDir, File file) {
		if(baseDir.equals(file)) {
			throw new IllegalArgumentException("BaseDir and File can't be the same.");
		}
		final String baseDirPath = canonicalPath(baseDir);
		final String filePath = canonicalPath(file);
		if (!filePath.startsWith(baseDirPath)) {
			return file.toURI().relativize(baseDir.toURI()).toString();
		}
		final String relativePath = filePath
				.substring(baseDirPath.length() + 1);
		return relativePath;
	}



	public static int copyDir(File source, File targetDir, FileFilter filter,
			boolean copyEmptyDir) {
		return copyDir(source, targetDir, filter, copyEmptyDir, false, null);
	}

	public static int copyDir(File source, File targetDir, FileFilter filterArg,
			boolean copyEmptyDir, boolean report, PrintStream reportStream) {
		return copyDirReplacingTokens(source, targetDir, filterArg, copyEmptyDir, report, reportStream, null);
	}

	public static int copyDirReplacingTokens(File fromFile, File toDir, FileFilter filterArg,
			boolean copyEmptyDir, boolean report, PrintStream reportStream, Map<String, String> tokenValues) {
		final FileFilter filter = JakeUtilsObject.firstNonNull(filterArg, JakeFileFilters.acceptAll());
		assertDir(fromFile);
		if (fromFile.equals(toDir)) {
			throw new IllegalArgumentException(
					"Base and destination directory can't be the same : "
							+ fromFile.getPath());
		}
		if (isAncestor(fromFile, toDir) && filter.accept(toDir)) {
			throw new IllegalArgumentException("Base filtered directory "
					+ fromFile.getPath() + ":(" + filter
					+ ") cannot contain destination directory "
					+ toDir.getPath()
					+ ". Narrow filter or change the target directory.");
		}
		if (toDir.isFile()) {
			throw new IllegalArgumentException(toDir.getPath()
					+ " is file. Should be directory");
		}

		final File[] children = fromFile.listFiles();
		int count = 0;
		for (final File child : children) {
			if (child.isFile()) {
				if (filter.accept(child)) {
					final File targetFile = new File(toDir, child.getName());
					if (tokenValues == null || tokenValues.isEmpty()) {
						copyFile(child, targetFile, report, reportStream);
					} else {
						final File toFile = new File(toDir, targetFile.getName());
						copyFileReplacingTokens(child, toFile, tokenValues, report, reportStream);
					}

					count++;
				}
			} else {
				final File subdir = new File(toDir, child.getName());
				if (filter.accept(child) && copyEmptyDir) {
					subdir.mkdirs();
				}
				final int subCount = copyDirReplacingTokens(child, subdir, filter,
						copyEmptyDir, report, reportStream, tokenValues);
				count = count + subCount;
			}

		}
		return count;
	}

	public static Properties readPropertyFile(File propertyfile)  {
		final Properties props = new Properties();
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(propertyfile);
			props.load(fileInputStream);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		} finally {
			JakeUtilsIO.closeQuietly(fileInputStream);
		}
		return props;
	}

	public static void copyFileToDir(File from, File toDir, boolean report, PrintStream reportStream) {
		final File to = new File(toDir, from.getName());
		copyFile(from, to, report, reportStream);
	}

	public static void copyFile(File from, File toFile) {
		copyFile(from, toFile, false, null);
	}

	public static void copyFile(File from, File toFile, boolean report, PrintStream reportStream) {
		if (!from.exists()) {
			throw new IllegalArgumentException("File " + from.getPath()
					+ " does not exist.");
		}
		if (from.isDirectory()) {
			throw new IllegalArgumentException(from.getPath()
					+ " is a directory. Should be a file.");
		}
		try {
			if (report) {
				reportStream.println("Coping file " + from.getAbsolutePath() + " to " + toFile.getAbsolutePath());
			}
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
					"IO exception occured while copying file " + from.getPath()
					+ " to " + toFile.getPath(), e);
		}

	}

	public static void deleteDirContent(File dir) {
		final File[] files = dir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					deleteDirContent(file);
				}
				file.delete();
			}
		}
	}

	public static String fileName(File anyFile) {
		final String absPath = canonicalPath(anyFile);
		final int index = absPath.lastIndexOf(File.separator);
		return absPath.substring(index);
	}

	public static String toPathString(Iterable<File> files, String separator) {
		final StringBuilder builder = new StringBuilder();
		final Iterator<File> fileIt = files.iterator();
		while (fileIt.hasNext()) {
			builder.append(fileIt.next().getAbsolutePath());
			if (fileIt.hasNext()) {
				builder.append(separator);
			}
		}
		return builder.toString();
	}

	public static URL toUrl(File file) {
		try {
			return file.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static List<File> toPath(String pathAsString, String separator,
			File baseDir) {
		final String[] paths = JakeUtilsString.split(pathAsString, separator);
		final List<File> result = new LinkedList<File>();
		for (final String path : paths) {
			File file = new File(path);
			if (!file.isAbsolute()) {
				file = new File(baseDir, path);
			}
			result.add(file);
		}
		return result;
	}

	public static boolean isAncestor(File ancestorCandidate,
			File childCandidtate) {
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
			throw new RuntimeException(e);
		}
	}

	public static void zipDir(File zipFile, int zipLevel, File... dirs) {
		zipDir(zipFile, zipLevel, Arrays.asList(dirs));
	}

	/**
	 * Zips the content of the specified directories into the specified zipFile.
	 * If the specified zip file does not exist, the method will create it.
	 * 
	 * @param zipLevel
	 *            the compression level (0-9) as specified in
	 *            {@link ZipOutputStream#setLevel(int)}.
	 */
	public static void zipDir(File zipFile, int zipLevel, Iterable<File> dirs) {

		final ZipOutputStream zos = JakeUtilsIO.createZipOutputStream(zipFile, zipLevel);
		try {
			for (final File dir : dirs) {
				JakeUtilsIO.addZipEntry(zos, dir, dir);
			}
			zos.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Returns all files contained recursively in the specified directory.
	 */
	public static List<File> filesOf(File dir, boolean includeFolder) {
		return filesOf(dir, JakeFileFilters.acceptAll(), includeFolder);
	}

	/**
	 * Returns all files contained recursively in the specified directory.
	 */
	public static List<File> filesOf(File dir, FileFilter fileFilter,
			boolean includeFolders) {
		assertDir(dir);
		final List<File> result = new LinkedList<File>();
		for (final File file : dir.listFiles()) {
			if (file.isFile() && !fileFilter.accept(file)) {
				continue;
			}
			if (file.isDirectory()) {
				if (includeFolders) {
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
	 * Returns count of files contained recursively in the specified directory.
	 * If the dir does not exist then it returns 0.
	 */
	public static int count(File dir, FileFilter fileFilter,
			boolean includeFolders) {
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

	public static File workingDir() {
		return JakeUtilsFile.canonicalFile(new File("."));
	}

	public static File tempDir() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

	public static void writeString(File file, String content, boolean append) {
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			final FileWriter fileWriter = new FileWriter(file, append);
			fileWriter.append(content);
			fileWriter.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static String md5Checksum(File file) {
		InputStream fis;
		try {
			fis = new FileInputStream(file);
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException(file.getPath() + " not found.", e);
		}

		final byte[] buffer = new byte[1024];
		MessageDigest complete;
		try {
			complete = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			JakeUtilsIO.closeQuietly(fis);
			throw new RuntimeException(e);
		}
		int numRead;
		do {
			numRead = JakeUtilsIO.read(fis);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		JakeUtilsIO.closeQuietly(fis);
		final byte[] bytes = complete.digest();
		String result = "";
		for (final byte element : bytes) {
			result += Integer.toString((element & 0xff) + 0x100, 16).substring(
					1);
		}
		return result;
	}

	public static File createTempFile(String prefix, String suffix) {
		try {
			return File.createTempFile(prefix, suffix);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void delete(File file) {
		if (!file.delete()) {
			throw new RuntimeException("File " + file.getAbsolutePath()  + " can't be deleted.");
		}
	}

	public static void copyFileReplacingTokens(File in, File out, Map<String, String> replacements) {
		copyFileReplacingTokens(in, out, replacements, false, null);
	}

	public static void copyFileReplacingTokens(File from, File toFile, Map<String, String> replacements, boolean report, PrintStream reportStream) {
		if (!from.exists()) {
			throw new IllegalArgumentException("File " + from.getPath()
					+ " does not exist.");
		}
		if (from.isDirectory()) {
			throw new IllegalArgumentException(from.getPath()
					+ " is a directory. Should be a file.");
		}
		final TokenReplacingReader replacingReader = new TokenReplacingReader(from, replacements);
		if (!toFile.exists()) {
			try {
				toFile.createNewFile();
			} catch (final IOException e) {
				JakeUtilsIO.closeQuietly(replacingReader);
				throw new RuntimeException(e);
			}
		}
		final Writer writer;
		try {
			writer = new FileWriter(toFile);
		} catch (final IOException e) {
			JakeUtilsIO.closeQuietly(replacingReader);
			throw new RuntimeException(e);
		}
		if (report) {
			reportStream.println("Coping and replacing token from file "
					+ from.getAbsolutePath() + " to " + toFile.getAbsolutePath());
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
			JakeUtilsIO.closeQuietly(writer);
			JakeUtilsIO.closeQuietly(replacingReader);
		}
	}



}
