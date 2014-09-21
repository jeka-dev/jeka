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

	public static JakeZip of(JakeDirSet ...jakeDirSets) {
		return new JakeZip(Arrays.asList(jakeDirSets));
	}

	public static JakeZip of(JakeDir ...jakeDirs) {
		return new JakeZip(Arrays.asList(jakeDirs));
	}


	@SuppressWarnings("unchecked")
	public JakeZip merge(Iterable<File> archiveFiles) {
		return new JakeZip(itemsToZip, JakeUtilsIterable.chain(this.archivestoMerge, archiveFiles));
	}

	public void create(File zipFile) {
		this.create(zipFile, Deflater.DEFAULT_COMPRESSION);
	}

	public void create(File zipFile, int compressLevel) {
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


}
