package org.jerkar.api.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

public class JkFileFilters {

	public static FileFilter endingBy(final String... suffixes) {
		return new FileFilter() {
	
			@Override
			public boolean accept(File file) {
				for (final String suffix : suffixes) {
					if (file.getName().endsWith(suffix)) {
						return true;
					}
				}
				return false;
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

}
