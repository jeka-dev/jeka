package org.jake.java;

import java.util.regex.Pattern;

/**
 * Filter on <code>Class</code> objects. 
 * 
 * @author Jerome Angibaud
 */
public abstract class ClassFilter {
	
	private static final ClassFilter ACCEPT_ALL = new ClassFilter() {

		@Override
		public boolean accept(Class<?> candidate) {
			return true;
		}
		
	};
	
	
	public abstract boolean accept(Class<?> candidate);
	
	public static ClassFilter acceptAll() {
		return ACCEPT_ALL;
	}
	
	public static ClassFilter endingBy(final String suffix) {
		return new ClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return candidate.getSimpleName().endsWith(suffix);
			}
			
		};
	}
	
	public static ClassFilter startWith(final String prefix) {
		return new ClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return candidate.getSimpleName().startsWith(prefix);
			}
			
		};
	}
	
	public static ClassFilter simpleNameMatching(final Pattern pattern) {
		return new ClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return pattern.matcher(candidate.getSimpleName()).find();
			}
			
		};
	}
	
	public static ClassFilter qualifiedNameMatching(final Pattern pattern) {
		return new ClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return pattern.matcher(candidate.getName()).find();
			}
			
		};
	}
	
	public ClassFilter union(final ClassFilter classFilter) {
		return new ClassFilter() {
			
			@Override
			public boolean accept(Class<?> candidate) {
				if (this.accept(candidate)) {
					return true;
				}
				return classFilter.accept(candidate);
			}
		};
	}
	
	public ClassFilter intersect(final ClassFilter classFilter) {
		return new ClassFilter() {
			
			@Override
			public boolean accept(Class<?> candidate) {
				if (!this.accept(candidate)) {
					return false;
				}
				return classFilter.accept(candidate);
			}
		};
	}
	

}