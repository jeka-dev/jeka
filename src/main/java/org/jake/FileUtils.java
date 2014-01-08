package org.jake;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

	public static void assertDir(File candidate) {
		if (!candidate.exists()) {
			throw new IllegalArgumentException(candidate.getAbsolutePath()
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
	
	

	public static int copyDir(File sourceLocation, File targetLocation,
			FilenameFilter filter) {
		int count = 0;
		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists()) {
				targetLocation.mkdir();
			}

			String[] children = sourceLocation.list(filter);
			for (int i = 0; i < children.length; i++) {
				int subCount = copyDir(new File(sourceLocation, children[i]), new File(
						targetLocation, children[i]), filter);
				count = count + subCount;
			}
			return count;
		} else {
			try {
				InputStream in = new FileInputStream(sourceLocation);
				OutputStream out = new FileOutputStream(targetLocation);

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			} catch (IOException e) {
				throw new RuntimeException(
						"IO exception occured while copying folder "
								+ sourceLocation.getAbsolutePath() + " to "
								+ targetLocation.getAbsolutePath(), e);
			}
			return 1;
		}
	}

	public static FilenameFilter endingBy(final String... suffixes) {
		return new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				for (String suffix : suffixes) {
					if (name.endsWith(suffix)) {
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
	
	public static boolean isParent(File parentCandidate, File childCandidtate) {
		File parent = childCandidtate;
		while (true) {
			parent = parent.getParentFile();
			if (parent == null) {
				return false;
			}
			if (parent.equals(parentCandidate)) {
				return true;
			}
		}
	}

	public static String canonicalPath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void zipDir(File zipFile, File dir, int level) {

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(zipFile);

			ZipOutputStream zos = new ZipOutputStream(fos);
			
			// level - the compression level (0-9)
			zos.setLevel(9);

			addFolder(zos, canonicalPath(dir), canonicalPath(dir));

			zos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static void addFolder(ZipOutputStream zos, String folderName,
			String baseFolderName) throws IOException {
		File f = new File(folderName);
		if (f.isDirectory()) {
			File f2[] = f.listFiles();
			for (int i = 0; i < f2.length; i++) {
				addFolder(zos, f2[i].getAbsolutePath(), baseFolderName);
			}
		} else {
			String entryName = folderName.substring(
					baseFolderName.length() + 1, folderName.length());
			ZipEntry ze = new ZipEntry(entryName);
			zos.putNextEntry(ze);
			FileInputStream in = new FileInputStream(folderName);
			int len;
			byte buffer[] = new byte[1024];
			while ((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			in.close();
			zos.closeEntry();
		}
	}
	
	public static FileSet fileSetOf(File dir) {
		final FileSet result = new FileSet();
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				result.addSingle(file);
				result.add(fileSetOf(file));
			} else {
				result.add(file);
			}
		}	
		return result;
	}

}
