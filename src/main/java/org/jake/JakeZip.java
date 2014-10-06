package org.jake;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;

/**
 * Defines element to embed in a zip archive and methods to write archive on disk.
 */
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

	/**
	 * Creates a {@link JakeZip} from an array of directories.
	 */
	public static JakeZip of(File ...dirs) {
		for (final File file : dirs) {
			if (!file.isDirectory()) {
				throw new IllegalArgumentException(file.getPath() + " is not a directory.");
			}
		}
		return new JakeZip(Arrays.asList( dirs));
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

	/**
	 * Zips and writes the contain of this archive to disk using {@link Deflater#DEFAULT_COMPRESSION} level.
	 * This method returns a {@link CheckSumer} to conveniently create digests of the produced zip file.
	 */
	public CheckSumer to(File zipFile) {
		return this.to(zipFile, Deflater.DEFAULT_COMPRESSION);
	}

	/**
	 * As {@link #to(File)} but specifying compression level.
	 */
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
				for (final JakeDir dirView : dirViews.jakeDirs()) {
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

	/**
	 * Wrapper on <code>File</code> allowing to creates digests on it.
	 * 
	 * @author Jerome Angibaud
	 */
	public static final class CheckSumer {

		/**
		 * Creates an instance of {@link CheckSumer} wrapping the specified file.
		 */
		public static CheckSumer of(File file) {
			return new CheckSumer(file);
		}

		private final File file;

		private CheckSumer(File file) {
			JakeUtilsAssert.isTrue(file.isFile(), file.getAbsolutePath() + " is a directory, not a file.");
			this.file = file;
		}

		/**
		 * Creates an MD5 digest for this wrapped file. The digest file is written in the same directory
		 * as the digested file and has the same name + '.md5' extension.
		 */
		public CheckSumer md5() {
			JakeLog.start("Creating MD5 file for : " + file);
			final File parent = file.getParentFile();
			final String md5 = JakeUtilsFile.md5Checksum(file);
			final String fileName = file.getName() + ".md5";
			JakeUtilsFile.writeString(new File(parent,  fileName), md5, false);
			JakeLog.done();
			return this;
		}

		/**
		 * As {@link #md5()} but allow to pass a flag as parameter to actually process or not the digesting.
		 */
		public CheckSumer md5(boolean process) {
			if (!process) {
				return this;
			}
			return md5();
		}
	}


}
