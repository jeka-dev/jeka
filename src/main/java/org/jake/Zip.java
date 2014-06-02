package org.jake;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jake.utils.FileUtils;

public class Zip {
	
	private final List<? extends Object> itemsToZip; 
	
	private final List<File> archivestoMerge;
	
	private Zip(List<? extends Object> itemsToZip, List<File> archivestoMerge) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = archivestoMerge;
	}
	
	private Zip(List<? extends Object> itemsToZip) {
		this.itemsToZip = itemsToZip;
		this.archivestoMerge = Collections.emptyList();
	}
	
	public static Zip of(File ...fileOrDirs) {
		return new Zip(Arrays.asList( fileOrDirs));
	}
	
	public static Zip of(DirView ...dirViews) {
		return new Zip(Arrays.asList(dirViews));
	}
	
	public static Zip of(DirViews ...dirViews) {
		return new Zip(Arrays.asList(dirViews));
	}
	
	public Zip and(List<File> files) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		final List<File> archives = new LinkedList<File>(this.archivestoMerge);
		items.addAll(files);
		return new Zip(items, archives);
	}
	
	public Zip and(File ...fileOrDirs) {
		return and(Arrays.asList(fileOrDirs));
	}
	
	public Zip and(DirView ...dirViews) {
		return and(DirViews.of(dirViews).listFiles());
		
	}
	
	public Zip and(DirViews ...dirViews) {
		return and(DirViews.toFiles(dirViews));
	}
	
	public Zip merge(List<File> archiveFiles) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		final List<File> archives = new LinkedList<File>(this.archivestoMerge);
		archives.addAll(archiveFiles);
		return new Zip(items, archives);
	}
	
	public void create(File zipFile, int compressLevel) {
		final ZipOutputStream zos = FileUtils.createZipOutputStream(zipFile, compressLevel);
		zos.setLevel(compressLevel);
		
		// Adding files to archive
		for (Object item : this.itemsToZip) {
			if (item instanceof File) {
				final File file = (File) item;
				if (file.isDirectory()) {
					FileUtils.addZipEntry(zos, file, file);
				} else {
					FileUtils.addZipEntry(zos, file, file);
				}
			} else if (item instanceof DirView) {
				final DirView dirView = (DirView) item;
				addDirView(zos, dirView);
			} else if (item instanceof DirViews) {
				final DirViews dirViews = (DirViews) item;
				for (DirView dirView : dirViews) {
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
			FileUtils.mergeZip(zos, file);
		}
		
		try {
			zos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void addDirView(ZipOutputStream zos, DirView dirView) {
		if (!dirView.exists()) {
			return;
		}
		final File base = FileUtils.canonicalFile(dirView.root());
		for (File file : dirView) {
			FileUtils.addZipEntry(zos, file, base);
		}
	}
	

}
