package org.jake.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
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
			throw new IllegalArgumentException("File " + filePath + " is not part of " + baseDirPath);
		}
		String relativePath = filePath.substring(baseDirPath.length() +1);
		return relativePath;
	}

	public static int copyDir(File source, File targetDir, FileFilter filter, boolean copyEmptyDir) {
		
		assertDir(source);
		if (source.equals(targetDir)) {
			throw new IllegalArgumentException(
					"Base and destination directory can't be the same : " + source.getPath());
		}
		if (isAncestor(source, targetDir) && filter.accept(targetDir)) {
			throw new IllegalArgumentException("Base filtered directory "
					+ source.getPath() + ":(" + filter
					+ ") cannot contain destination directory "
					+ targetDir.getPath()
					+ ". Narrow filter or change the target directory.");
		}
		if (targetDir.isFile()) {
			throw new IllegalArgumentException(targetDir.getPath() + " is file. Should be directory");
		}
		
				
		final File[] children = source.listFiles();
		int count = 0;
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
				if (child.isFile()) {
					if (filter.accept(child)) {
						final File targetFile = new File(targetDir, child.getName());
					    copyFile(child, targetFile);
					    count ++;
					}
				} else { 
					final File subdir = new File(targetDir, child.getName());
					if (filter.accept(subdir) && copyEmptyDir) {
						subdir.mkdirs();
					}
					int subCount = copyDir(	child, subdir, filter, copyEmptyDir);
					count = count + subCount;
				}
				
		}
		return count;
	}
	
	public static void copyFile(File from, File to) {
		if (!from.exists()) {
			throw new IllegalArgumentException("File " + from.getPath()+ " does not exist.");
		}
		if (from.isDirectory()) {
			throw new IllegalArgumentException(from.getPath() + " is a directory. Should be a path.");
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
					"IO exception occured while copying file "
							+ from.getPath() + " to "
							+ to.getPath(), e);
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

	public static FileFilter asIncludeFileFilter(final String... antPatterns) {
		return new FileFilter() {

			@Override
			public boolean accept(File candidate) {
				for (final String antPattern : antPatterns) {
					boolean match = AntPatternUtils.doMatch(antPattern, candidate.getPath());
					if (match) {
						return true;
					}
				}
				return false;
			}

			@Override
			public String toString() {
				return "includes" + Arrays.toString(antPatterns);
			}
		};
	}

	public static FileFilter asExcludeFileFilter(final String... antPatterns) {
		return new FileFilter() {

			@Override
			public boolean accept(File candidate) {
				for (final String antPattern : antPatterns) {
					if (AntPatternUtils
							.doMatch(antPattern, candidate.getPath())) {
						return false;
					}
				}
				return true;
			}

			@Override
			public String toString() {
				return "excludes" + Arrays.toString(antPatterns);
			}
		};
	}

	public static FileFilter noneFileFilter() {
		return new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return true;
			}

			@Override
			public String toString() {
				return "Nope Filter";
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

		FileOutputStream fos;
		try {
			if (!zipFile.exists()) {
				zipFile.createNewFile();
			}
			fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			zos.setLevel(zipLevel);
			for (File dir : dirs) {
				addFolder(zos, canonicalPath(dir), canonicalPath(dir));
			}
			zos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static void addFolder(ZipOutputStream zos, String fileName,
			String baseFolderName) throws IOException {

		final File fileToZip = new File(fileName);
		if (fileToZip.isDirectory()) {
			final File[] files = fileToZip.listFiles();
			for (int i = 0; i < files.length; i++) {
				addFolder(zos, files[i].getAbsolutePath(), baseFolderName);
			}
		} else {
			String entryName = fileName.substring(baseFolderName.length() + 1,
					fileName.length());
			entryName = entryName.replace(File.separatorChar, '/');

			ZipEntry zipEntry = new ZipEntry(entryName);
			try {
				zos.putNextEntry(zipEntry);
			} catch (ZipException e) {

				// Ignore duplicate entry - no overwriting
				return;
			}
			FileInputStream in = new FileInputStream(fileName);
			int buffer = 2048;
			BufferedInputStream bufferedInputStream = new BufferedInputStream(
					in, buffer);
			int count;
			byte data[] = new byte[buffer];
			while ((count = bufferedInputStream.read(data, 0, buffer)) != -1) {
				zos.write(data, 0, count);
			}
			bufferedInputStream.close();
			in.close();
			zos.closeEntry();
		}
	}

	/**
	 * Returns all files contained recursively in the specified directory.
	 */
	public static List<File> filesOf(File dir, boolean includeFolder) {
		return filesOf(dir, noneFileFilter(), includeFolder);
	}

	/**
	 * Returns all files contained recursively in the specified directory.
	 * Folders are not returned.
	 */
	public static List<File> filesOf(File dir, FileFilter fileFilter, boolean includeFolders) {
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

}
