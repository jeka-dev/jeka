package org.jerkar.java;

import java.util.regex.Pattern;

/**
 * Filter on <code>Class</code> objects.
 * 
 * @author Jerome Angibaud
 */
public abstract class JkClassFilter {

	private static final JkClassFilter ACCEPT_ALL = new JkClassFilter() {

		@Override
		public boolean accept(Class<?> candidate) {
			return true;
		}

	};


	public abstract boolean accept(Class<?> candidate);

	public static JkClassFilter acceptAll() {
		return ACCEPT_ALL;
	}

	public static JkClassFilter endingBy(final String suffix) {
		return new JkClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return candidate.getSimpleName().endsWith(suffix);
			}

		};
	}

	public static JkClassFilter startWith(final String prefix) {
		return new JkClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return candidate.getSimpleName().startsWith(prefix);
			}

		};
	}

	public static JkClassFilter simpleNameMatching(final Pattern pattern) {
		return new JkClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return pattern.matcher(candidate.getSimpleName()).find();
			}

		};
	}

	public static JkClassFilter qualifiedNameMatching(final Pattern pattern) {
		return new JkClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return pattern.matcher(candidate.getName()).find();
			}

		};
	}

	public JkClassFilter union(final JkClassFilter classFilter) {
		return new JkClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				if (JkClassFilter.this.accept(candidate)) {
					return true;
				}
				return classFilter.accept(candidate);
			}
		};
	}

	public JkClassFilter intersect(final JkClassFilter classFilter) {
		return new JkClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				if (!JkClassFilter.this.accept(candidate)) {
					return false;
				}
				return classFilter.accept(candidate);
			}
		};
	}


}