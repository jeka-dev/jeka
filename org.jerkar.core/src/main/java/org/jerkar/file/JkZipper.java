package org.jerkar.file;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jerkar.JkLog;
import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;
import org.jerkar.utils.JkUtilsIterable;

/**
 * Defines elements to embed in a zip archive and methods to write archive on disk.
 */
public final class JkZipper {

	private final List<? extends Object> itemsToZip;

	private final List<File> archivestoMerge;

	private JkZipper(List<? extends Object> itemsToZip, List<File> archivestoMerge) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = archivestoMerge;
	}

	private JkZipper(List<? extends Object> itemsToZip) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = Collections.emptyList();
	}

	/**
	 * Creates a {@link JkZipper} from an array of directories.
	 */
	public static JkZipper of(File ...dirs) {
		for (final File file : dirs) {
			if (!file.isDirectory()) {
				throw new IllegalArgumentException(file.getPath() + " is not a directory.");
			}
		}
		return new JkZipper(Arrays.asList( dirs));
	}

	static JkZipper of(JkFileTreeSet ...jkDirSets) {
		return new JkZipper(Arrays.asList(jkDirSets));
	}

	static JkZipper of(JkFileTree ...jkDirs) {
		return new JkZipper(Arrays.asList(jkDirs));
	}


	@SuppressWarnings("unchecked")
	public JkZipper merge(Iterable<File> archiveFiles) {
		return new JkZipper(itemsToZip, JkUtilsIterable.concatLists(this.archivestoMerge, archiveFiles));
	}

	public JkZipper merge(File... archiveFiles) {
		return merge(Arrays.asList(archiveFiles));
	}

	/**
	 * Zips and writes the contain of this archive to disk using {@link Deflater#DEFAULT_COMPRESSION} level.
	 * This method returns a {@link CheckSumer} to conveniently create digests of the produced zip file.
	 */
	public CheckSumer to(File zipFile) {
		JkUtilsFile.createFileIfNotExist(zipFile);
		return this.to(zipFile, Deflater.DEFAULT_COMPRESSION);
	}

	public CheckSumer appendTo(File existingARchive) {
		return this.appendTo(existingARchive, Deflater.DEFAULT_COMPRESSION);
	}

	public CheckSumer appendTo(File existingARchive, int compressionLevel) {
		final File temp = JkUtilsFile.createTempFile(existingARchive.getName(), "");
		JkUtilsFile.move(existingARchive, temp);
		final CheckSumer checkSumer = this.merge(temp).to(existingARchive, compressionLevel);
		temp.delete();
		return checkSumer;
	}

	/**
	 * As {@link #to(File)} but specifying compression level.
	 */
	public CheckSumer to(File zipFile, int compressLevel) {
		JkLog.start("Creating zip file : " + zipFile);
		final ZipOutputStream zos = JkUtilsIO.createZipOutputStream(zipFile, compressLevel);
		zos.setLevel(compressLevel);

		// Adding files to archive
		for (final Object item : this.itemsToZip) {
			if (item instanceof File) {
				final File file = (File) item;
				JkUtilsIO.addZipEntry(zos, file, file.getParentFile());
			} else if (item instanceof JkFileTree) {
				final JkFileTree dirView = (JkFileTree) item;
				addDirView(zos, dirView);
			} else if (item instanceof JkFileTreeSet) {
				final JkFileTreeSet dirViews = (JkFileTreeSet) item;
				for (final JkFileTree dirView : dirViews.fileTrees()) {
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
				throw new RuntimeException("Error while opening zip file " + archiveToMerge.getPath(), e);
			}
			JkUtilsIO.mergeZip(zos, file);
		}

		try {
			zos.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		JkLog.done();
		return new CheckSumer(zipFile);
	}

	private void addDirView(ZipOutputStream zos, JkFileTree dirView) {
		if (!dirView.exists()) {
			return;
		}
		final File base = JkUtilsFile.canonicalFile(dirView.root());
		for (final File file : dirView) {
			JkUtilsIO.addZipEntry(zos, file, base);
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
			JkUtilsAssert.isTrue(file.isFile(), file.getAbsolutePath() + " is a directory, not a file.");
			this.file = file;
		}

		/**
		 * Creates an MD5 digest for this wrapped file. The digest file is written in the same directory
		 * as the digested file and has the same name + '.md5' extension.
		 */
		public CheckSumer md5() {
			JkLog.start("Creating MD5 file for : " + file);
			final File parent = file.getParentFile();
			final String md5 = JkUtilsFile.md5Checksum(file);
			final String fileName = file.getName() + ".md5";
			JkUtilsFile.writeString(new File(parent,  fileName), md5, false);
			JkLog.done();
			return this;
		}

		/**
		 * As {@link #md5()} but allow to pass a flag as parameter to actually process or not the digesting.
		 */
		public CheckSumer md5If(boolean process) {
			if (!process) {
				return this;
			}
			return md5();
		}
	}


}
