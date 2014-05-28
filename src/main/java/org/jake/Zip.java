package org.jake;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.jake.utils.FileUtils;

public class Zip {
	
	private final List<? extends Object> itemsToZip; 
		
	private Zip(List<? extends Object> itemsToZip) {
		this.itemsToZip = itemsToZip;
	}
	
	public static Zip of(File ...fileOrDirs) {
		return new Zip(Arrays.asList( fileOrDirs) );
	}
	
	public static Zip of(DirView ...dirViews) {
		return new Zip(Arrays.asList(dirViews));
	}
	
	public static Zip of(DirViews ...dirViews) {
		return new Zip(Arrays.asList(dirViews));
	}
	
	public Zip and(File ...fileOrDirs) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		items.addAll(Arrays.asList(fileOrDirs));
		return new Zip(items);
	}
	
	public Zip and(DirView ...dirViews) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		items.addAll(Arrays.asList(dirViews));
		return new Zip(items);
	}
	
	public Zip and(DirViews ...dirViews) {
		final List<Object> items = new LinkedList<Object>(this.itemsToZip);
		items.addAll(Arrays.asList(dirViews));
		return new Zip(items);
	}
	
	public void create(File zipFile, int compressLevel) {
		final ZipOutputStream zos = FileUtils.createZipOutputStream(zipFile, compressLevel);
		zos.setLevel(compressLevel);
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
