package org.jake;

import java.io.FileFilter;

import org.jake.utils.FileUtils;

public class Filter {
	
	private final FileFilter fileFilter ;

	private Filter(FileFilter fileFilter) {
		super();
		this.fileFilter = fileFilter;
	}
	
	public static Filter of(FileFilter fileFilter) {
		return new Filter(fileFilter);
	}
	
	public static Filter include(String ... antPatterns) {
		return new Filter(FileUtils.asIncludeFileFilter(antPatterns));
	}
	
	public static Filter exclude(String ... antPatterns) {
		return new Filter(FileUtils.asExcludeFileFilter(antPatterns));
	}
	
	public static Filter acceptAll() {
		return new Filter(FileUtils.acceptAll());
	}
	
	
	public Filter andIncludeOnly(String ... antPatterns) {
		return new Filter(FileUtils.combine(this.fileFilter, 
				include(antPatterns).fileFilter));
	}
	
	public Filter andExcludeAll(String ... antPatterns) {
		return new Filter(FileUtils.combine(this.fileFilter, 
				exclude(antPatterns).fileFilter));
	}
	
	public Filter combine(Filter other) {
		return new Filter(FileUtils.combine(this.fileFilter, other.fileFilter));
	}
	
	public Filter reverse() {
		return new Filter(FileUtils.reverse(this.fileFilter));
	}
	
	public FileFilter fileFilter() {
		return fileFilter;
	}
	
	@Override
	public String toString() {
		return fileFilter.toString();
	}
	
}
