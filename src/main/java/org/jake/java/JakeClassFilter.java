package org.jake.java;

import java.util.regex.Pattern;

/**
 * Filter on <code>Class</code> objects.
 * 
 * @author Djeang
 */
public abstract class JakeClassFilter {

	private static final JakeClassFilter ACCEPT_ALL = new JakeClassFilter() {

		@Override
		public boolean accept(Class<?> candidate) {
			return true;
		}

	};


	public abstract boolean accept(Class<?> candidate);

	public static JakeClassFilter acceptAll() {
		return ACCEPT_ALL;
	}

	public static JakeClassFilter endingBy(final String suffix) {
		return new JakeClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return candidate.getSimpleName().endsWith(suffix);
			}

		};
	}

	public static JakeClassFilter startWith(final String prefix) {
		return new JakeClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return candidate.getSimpleName().startsWith(prefix);
			}

		};
	}

	public static JakeClassFilter simpleNameMatching(final Pattern pattern) {
		return new JakeClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return pattern.matcher(candidate.getSimpleName()).find();
			}

		};
	}

	public static JakeClassFilter qualifiedNameMatching(final Pattern pattern) {
		return new JakeClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				return pattern.matcher(candidate.getName()).find();
			}

		};
	}

	public JakeClassFilter union(final JakeClassFilter classFilter) {
		return new JakeClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				if (JakeClassFilter.this.accept(candidate)) {
					return true;
				}
				return classFilter.accept(candidate);
			}
		};
	}

	public JakeClassFilter intersect(final JakeClassFilter classFilter) {
		return new JakeClassFilter() {

			@Override
			public boolean accept(Class<?> candidate) {
				if (!JakeClassFilter.this.accept(candidate)) {
					return false;
				}
				return classFilter.accept(candidate);
			}
		};
	}


}