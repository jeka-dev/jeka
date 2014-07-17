package org.jake.file;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jake.file.utils.JakeUtilsFile;

public class JakeZip {
	
	private final List<? extends Object> itemsToZip; 
	
	private final List<File> archivestoMerge;
	
	private JakeZip(List<? extends Object> itemsToZip, List<File> archivestoMerge) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = archivestoMerge;
	}
	
	private JakeZip(List<? extends Object> itemsToZip) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = Collections.emptyList();
	}
	
	public static JakeZip of(File ...fileOrDirs) {
		return new JakeZip(Arrays.asList( fileOrDirs));
	}
	
	public static JakeZip of(JakeDirView ...dirViews) {
		return new JakeZip(Arrays.asList(dirViews));
	}
	
	public static JakeZip of(JakeDirViewSet ...dirViews) {
		return new JakeZip(Arrays.asList(dirViews));
	}
	
	public JakeZip and(List<File> files) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		final List<File> archives = new LinkedList<File>(this.archivestoMerge);
		items.addAll(files);
		return new JakeZip(items, archives);
	}
	
	public JakeZip and(File ...fileOrDirs) {
		return and(Arrays.asList(fileOrDirs));
	}
	
	public JakeZip and(JakeDirView ...dirViews) {
		return and(JakeDirViewSet.of(dirViews).listFiles());
		
	}
	
	public JakeZip and(JakeDirViewSet ...dirViews) {
		return and(JakeDirViewSet.toFiles(dirViews));
	}
	
	public JakeZip merge(List<File> archiveFiles) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		final List<File> archives = new LinkedList<File>(this.archivestoMerge);
		archives.addAll(archiveFiles);
		return new JakeZip(items, archives);
	}
	
	public void create(File zipFile) {
		this.create(zipFile, Deflater.DEFAULT_COMPRESSION);
	}
	
	public void create(File zipFile, int compressLevel) {
		final ZipOutputStream zos = JakeUtilsFile.createZipOutputStream(zipFile, compressLevel);
		zos.setLevel(compressLevel);
		
		// Adding files to archive
		for (Object item : this.itemsToZip) {
			if (item instanceof File) {
				final File file = (File) item;
				if (file.isDirectory()) {
					JakeUtilsFile.addZipEntry(zos, file, file);
				} else {
					JakeUtilsFile.addZipEntry(zos, file, file);
				}
			} else if (item instanceof JakeDirView) {
				final JakeDirView dirView = (JakeDirView) item;
				addDirView(zos, dirView);
			} else if (item instanceof JakeDirViewSet) {
				final JakeDirViewSet dirViews = (JakeDirViewSet) item;
				for (JakeDirView dirView : dirViews) {
					addDirView(zos, dirView);
				}
			} else {
				throw new IllegalStateException("Items of class " + item.getClass() + " not handled.");
			}
		}
		
		// Merging archives to this archive
		for (File archiveToMerge : this.archivestoMerge) {
			final ZipFile file;
			try {
				file = new ZipFile(archiveToMerge);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			JakeUtilsFile.mergeZip(zos, file);
		}
		
		try {
			zos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void addDirView(ZipOutputStream zos, JakeDirView dirView) {
		if (!dirView.exists()) {
			return;
		}
		final File base = JakeUtilsFile.canonicalFile(dirView.root());
		for (File file : dirView) {
			JakeUtilsFile.addZipEntry(zos, file, base);
		}
	}
	

}
