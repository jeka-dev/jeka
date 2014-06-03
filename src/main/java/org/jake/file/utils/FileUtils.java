package org.jake.file.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtils {

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

	public static List<File> sum(List<File>... files) {
		List<File> result = new LinkedList<File>();
		for (List<File> list : files) {
			result.addAll(list);
		}
		return result;
	}

	public static String getRelativePath(File baseDir, File file) {
		String baseDirPath = canonicalPath(baseDir);
		String filePath = canonicalPath(file);
		if (!filePath.startsWith(baseDirPath)) {
			throw new IllegalArgumentException("File " + filePath
					+ " is not part of " + baseDirPath);
		}
		String relativePath = filePath.substring(baseDirPath.length() + 1);
		return relativePath;
	}

	public static int copyDir(File source, File targetDir, FileFilter filter,
			boolean copyEmptyDir) {

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
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
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
				int subCount = copyDir(child, subdir, filter, copyEmptyDir);
				count = count + subCount;
			}

		}
		return count;
	}

	public static void copyFile(File from, File to) {
		if (!from.exists()) {
			throw new IllegalArgumentException("File " + from.getPath()
					+ " does not exist.");
		}
		if (from.isDirectory()) {
			throw new IllegalArgumentException(from.getPath()
					+ " is a directory. Should be a path.");
		}
		try {
			InputStream in = new FileInputStream(from);
			if (!to.getParentFile().exists()) {
				to.getParentFile().mkdirs();
			}
			if (!to.exists()) {
				to.createNewFile();
			}
			OutputStream out = new FileOutputStream(to);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(
					"IO exception occured while copying file " + from.getPath()
							+ " to " + to.getPath(), e);
		}

	}

	public static FileFilter endingBy(final String... suffixes) {
		return new FileFilter() {

			@Override
			public boolean accept(File file) {
				for (String suffix : suffixes) {
					if (file.getName().endsWith(suffix)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	public static FilenameFilter reverse(final FilenameFilter filter) {
		return new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !filter.accept(dir, name);
			}
		};
	}

	public static FileFilter reverse(final FileFilter filter) {
		return new FileFilter() {

			@Override
			public boolean accept(File candidate) {
				return !filter.accept(candidate);
			}

			@Override
			public String toString() {
				return "revert of (" + filter + ")";
			}
		};
	}

	public static FileFilter combine(final FileFilter filter1,
			final FileFilter filter2) {
		return new FileFilter() {

			@Override
			public boolean accept(File candidate) {
				return filter1.accept(candidate) && filter2.accept(candidate);
			}

			@Override
			public String toString() {
				return "{" + filter1 + "," + filter2 + "}";
			}
		};
	}

	

	public static FileFilter acceptAll() {
		return new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return true;
			}

			@Override
			public String toString() {
				return "Accept-All filter";
			}
		};
	}

	public static FileFilter acceptOnly(final File fileToAccept) {
		return new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.equals(fileToAccept);
			}

			@Override
			public String toString() {
				return "Accept only " + fileToAccept.getAbsolutePath();
			}
		};
	}

	public static void deleteDirContent(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirContent(file);
				}
				file.delete();
			}
		}
	}

	public static String fileName(File anyFile) {
		String absPath = canonicalPath(anyFile);
		int index = absPath.lastIndexOf(File.separator);
		return absPath.substring(index);
	}

	public static String asPath(Iterable<File> files, String separator) {
		StringBuilder builder = new StringBuilder();
		Iterator<File> fileIt = files.iterator();
		while (fileIt.hasNext()) {
			builder.append(fileIt.next().getAbsolutePath());
			if (fileIt.hasNext()) {
				builder.append(separator);
			}
		}
		return builder.toString();
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A 'checked exception free' version of {@link File#getCanonicalFile()}.
	 */
	public static File canonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		} catch (IOException e) {
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

		final ZipOutputStream zos = createZipOutputStream(zipFile, zipLevel);
		try {
			for (File dir : dirs) {
				addZipEntry(zos, dir, dir);
			}
			zos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static ZipOutputStream createZipOutputStream(File file,
			int compressLevel) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			final FileOutputStream fos = new FileOutputStream(file);
			final ZipOutputStream zos = new ZipOutputStream(fos);
			zos.setLevel(compressLevel);
			return zos;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	

	public static Set<String> mergeZip(ZipOutputStream zos, ZipFile zipFile) {
		final Set<String> duplicateEntries = new HashSet<String>();
		final Enumeration<? extends ZipEntry> entries = zipFile.entries();
	    while (entries.hasMoreElements()) {
	    	final ZipEntry e = entries.nextElement();
	    	try {
				if (!e.isDirectory()) {
		    		boolean success = addEntryInputStream(zos, e.getName(), zipFile.getInputStream(e));;
		    		if (!success) {
		    			duplicateEntries.add(e.getName());
		    		}
				}
			} catch (IOException e1) {
				throw new RuntimeException("Error while merging entry " + e.getName() + " from zip file " + zipFile.getName(), e1);
			}
	    }
	    return duplicateEntries;
	}


	/**
	 * Add a zip entry into the provided <code>ZipOutputStream</code>. The zip
	 * entry is the part of <code>filePathToZip</code> truncated with the
	 * <code>baseFolderPath</code>.
	 * <p>
	 * So a file or folder <code>c:\my\base\folder\my\file\to\zip.txt</code>
	 * will be added in archive using <code>my/file/to/zip.txt</code> entry.
	 */
	public static void addZipEntry(ZipOutputStream zos, File fileToZip,
			File baseFolder) {

		if (fileToZip.isDirectory()) {
			final File[] files = fileToZip.listFiles();
			for (int i = 0; i < files.length; i++) {
				addZipEntry(zos, files[i], baseFolder);
			}
		} else {
			String filePathToZip = canonicalPath(fileToZip);
			String entryName = filePathToZip.substring(
					canonicalPath(baseFolder).length() + 1, filePathToZip.length());
			entryName = entryName.replace(File.separatorChar, '/');
			final FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(filePathToZip);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
			addEntryInputStream(zos, entryName, inputStream);
		}
	}
	

	private static boolean addEntryInputStream(ZipOutputStream zos, String entryName, InputStream inputStream) {
		final ZipEntry zipEntry = new ZipEntry(entryName);
		try {
			zos.putNextEntry(zipEntry);
		} catch (ZipException e) {

			// Ignore duplicate entry - no overwriting
			return false;
		} catch (IOException e) {
			throw new RuntimeException("Error while adding zip entry "
					+ zipEntry, e);
		}
		final int buffer = 2048;
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(
				inputStream, buffer);
		int count;
		try {
			byte data[] = new byte[buffer];
			while ((count = bufferedInputStream.read(data, 0, buffer)) != -1) {
				zos.write(data, 0, count);
			}
			bufferedInputStream.close();
			inputStream.close();
			zos.closeEntry();
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static void closeZipEntryQuietly(ZipOutputStream outputStream) {
		try {
			outputStream.closeEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	} 
	
	


	/**
	 * Returns all files contained recursively in the specified directory.
	 */
	public static List<File> filesOf(File dir, boolean includeFolder) {
		return filesOf(dir, acceptAll(), includeFolder);
	}

	/**
	 * Returns all files contained recursively in the specified directory.
	 */
	public static List<File> filesOf(File dir, FileFilter fileFilter,
			boolean includeFolders) {
		assertDir(dir);
		final List<File> result = new LinkedList<File>();
		for (File file : dir.listFiles()) {
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
	 */
	public static int count(File dir, FileFilter fileFilter,
			boolean includeFolders) {
		int result = 0;
		for (File file : dir.listFiles()) {
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

}
