package org.jake.file.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsString;

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

	public static String getRelativePath(File baseDir, File file) {
		final String baseDirPath = canonicalPath(baseDir);
		final String filePath = canonicalPath(file);
		if (!filePath.startsWith(baseDirPath)) {
			throw new IllegalArgumentException("File " + filePath
					+ " is not part of " + baseDirPath);
		}
		final String relativePath = filePath
				.substring(baseDirPath.length() + 1);
		return relativePath;
	}

	public static int copyDir(File source, File targetDir, FileFilter filter,
			boolean copyEmptyDir) {
		if (filter == null) {
			filter = JakeFileFilters.acceptAll();
		}
		assertDir(source);
		if (source.equals(targetDir)) {
			throw new IllegalArgumentException(
					"Base and destination directory can't be the same : "
							+ source.getPath());
		}
		if (isAncestor(source, targetDir) && filter.accept(targetDir)) {
			throw new IllegalArgumentException("Base filtered directory "
					+ source.getPath() + ":(" + filter
					+ ") cannot contain destination directory "
					+ targetDir.getPath()
					+ ". Narrow filter or change the target directory.");
		}
		if (targetDir.isFile()) {
			throw new IllegalArgumentException(targetDir.getPath()
					+ " is file. Should be directory");
		}

		final File[] children = source.listFiles();
		int count = 0;
		for (final File child : children) {
			if (child.isFile()) {
				if (filter.accept(child)) {
					final File targetFile = new File(targetDir, child.getName());
					copyFile(child, targetFile);
					count++;
				}
			} else {
				final File subdir = new File(targetDir, child.getName());
				if (filter.accept(child) && copyEmptyDir) {
					subdir.mkdirs();
				}
				final int subCount = copyDir(child, subdir, filter,
						copyEmptyDir);
				count = count + subCount;
			}

		}
		return count;
	}

	public static void copyFileToDir(File from, File toDir) {
		final File to = new File(toDir, from.getName());
		copyFile(from, to);
	}

	public static void copyFile(File from, File toFile) {
		if (!from.exists()) {
			throw new IllegalArgumentException("File " + from.getPath()
					+ " does not exist.");
		}
		if (from.isDirectory()) {
			throw new IllegalArgumentException(from.getPath()
					+ " is a directory. Should be a file.");
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

	public static void writeString(File file, String content, boolean append) {
		try {
			if (!file.exists()) {
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



}
