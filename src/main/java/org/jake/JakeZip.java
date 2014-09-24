package org.jake;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

public final class JakeZip {

	private final Iterable<? extends Object> itemsToZip;

	private final Iterable<File> archivestoMerge;

	private JakeZip(Iterable<? extends Object> itemsToZip, Iterable<File> archivestoMerge) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = archivestoMerge;
	}

	private JakeZip(Iterable<? extends Object> itemsToZip) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = Collections.emptyList();
	}

	public static JakeZip of(File ...fileOrDirs) {
		return new JakeZip(Arrays.asList( fileOrDirs));
	}

	static JakeZip of(JakeDirSet ...jakeDirSets) {
		return new JakeZip(Arrays.asList(jakeDirSets));
	}

	static JakeZip of(JakeDir ...jakeDirs) {
		return new JakeZip(Arrays.asList(jakeDirs));
	}


	@SuppressWarnings("unchecked")
	public JakeZip merge(Iterable<File> archiveFiles) {
		return new JakeZip(itemsToZip, JakeUtilsIterable.chain(this.archivestoMerge, archiveFiles));
	}

	public CheckSumer to(File zipFile) {
		this.to(zipFile, Deflater.DEFAULT_COMPRESSION);
		return new CheckSumer(zipFile);
	}

	public CheckSumer to(File zipFile, int compressLevel) {
		JakeLog.start("Creating zip file : " + zipFile);
		final ZipOutputStream zos = JakeUtilsIO.createZipOutputStream(zipFile, compressLevel);
		zos.setLevel(compressLevel);

		// Adding files to archive
		for (final Object item : this.itemsToZip) {
			if (item instanceof File) {
				final File file = (File) item;
				JakeUtilsIO.addZipEntry(zos, file, file.getParentFile());
			} else if (item instanceof JakeDir) {
				final JakeDir dirView = (JakeDir) item;
				addDirView(zos, dirView);
			} else if (item instanceof JakeDirSet) {
				final JakeDirSet dirViews = (JakeDirSet) item;
				for (final JakeDir dirView : dirViews.listJakeDirs()) {
					addDirView(zos, dirView);
				}
			} else {
				throw new IllegalStateException("Items of class " + item.getClass() + " not handled.");
			}
		}

		// Merging archives to this archive
		for (final File archiveToMerge : this.archivestoMerge) {
			final ZipFile file;
			try {
				file = new ZipFile(archiveToMerge);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			JakeUtilsIO.mergeZip(zos, file);
		}

		try {
			zos.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		JakeLog.done();
		return new CheckSumer(zipFile);
	}

	private void addDirView(ZipOutputStream zos, JakeDir dirView) {
		if (!dirView.exists()) {
			return;
		}
		final File base = JakeUtilsFile.canonicalFile(dirView.root());
		for (final File file : dirView) {
			JakeUtilsIO.addZipEntry(zos, file, base);
		}
	}

	public static final class CheckSumer {

		private final File file;

		public CheckSumer(File file) {
			super();
			this.file = file;
		}

		public CheckSumer md5() {
			JakeLog.start("Creating MD5 file for : " + file);
			final File parent = file.getParentFile();
			final String md5 = JakeUtilsFile.md5Checksum(file);
			final String fileName = JakeUtilsString.substringBeforeLast(file.getName(), ".") + ".md5";
			JakeUtilsFile.writeString(new File(parent,  fileName), md5, false);
			JakeLog.done();
			return this;
		}

		public CheckSumer md5(boolean process) {
			if (!process) {
				return this;
			}
			return md5();
		}
	}


}
