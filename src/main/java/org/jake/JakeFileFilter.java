package org.jake;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

import org.jake.utils.JakeUtilsFile;

/**
 * Filter on relative path ala <href a='https://ant.apache.org/manual/Types/patternset.html'>Ant pattern</href>.
 */
public abstract class JakeFileFilter {

	/**
	 * Filter accepting all.
	 */
	public static final JakeFileFilter ACCEPT_ALL = new JakeFileFilter() {

		@Override
		public boolean accept(String relativePath) {
			return true;
		}

		@Override
		public String toString() {
			return "Accept all";
		};

	};

	/**
	 * Returns if this filter should accept the specified relative path.
	 * @param relativePath
	 * @return
	 */
	public abstract boolean accept(String relativePath);

	/**
	 * Creates an include filter including the specified and patterns.
	 */
	public static JakeFileFilter include(String ... antPatterns) {
		return new IncludeFilter(AntPattern.setOf(antPatterns));
	}

	/**
	 * Creates an include filter excluding the specified and patterns.
	 */
	public static JakeFileFilter exclude(String ... antPatterns) {
		return new ExcludeFilter(AntPattern.setOf(antPatterns));
	}

	/**
	 * Creates a filter made of this one plus the specified include ones.
	 */
	public JakeFileFilter andInclude(String ... antPatterns) {
		return this.and(include(antPatterns));
	}

	/**
	 * Creates a filter made of this one plus the specified exclude ones.
	 */
	public JakeFileFilter andExclude(String ... antPatterns) {
		return this.and(exclude(antPatterns));
	}

	/**
	 * Creates a filter made of this one plus the specified one.
	 */
	public JakeFileFilter and(JakeFileFilter other) {
		return and(this, other);
	}

	/**
	 * Creates a filter which is the inverse of this one.
	 */
	public JakeFileFilter reverse() {
		return new JakeFileFilter() {

			@Override
			public boolean accept(String relativePath) {
				return !JakeFileFilter.this.accept(relativePath);
			}

		};
	}

	/**
	 * Creates a {@link FileFilter} base on this relative path filter and the specified
	 * base directory on which relative path are transformed to absolute.
	 */
	public FileFilter toFileFilter(final File baseDir) {
		return new FileFilter() {

			@Override
			public boolean accept(File file) {
				final String relativePath = JakeUtilsFile.getRelativePath(baseDir, file);
				return JakeFileFilter.this.accept(relativePath);
			}
		};
	}


	private static final class IncludeFilter extends JakeFileFilter {

		private final Set<AntPattern> antPatterns;

		private IncludeFilter(Set<AntPattern> antPatterns) {
			super();
			this.antPatterns = antPatterns;
		}

		@Override
		public boolean accept(String relativePath) {

			for (final AntPattern antPattern : antPatterns) {
				final boolean match = antPattern.doMatch(relativePath);
				if (match) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "includes " + antPatterns;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((antPatterns == null) ? 0 : antPatterns.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final IncludeFilter other = (IncludeFilter) obj;
			if (antPatterns == null) {
				if (other.antPatterns != null) {
					return false;
				}
			} else if (!antPatterns.equals(other.antPatterns)) {
				return false;
			}
			return true;
		}


	}

	private static class ExcludeFilter extends JakeFileFilter {


		private final Set<AntPattern> antPatterns;

		private ExcludeFilter(Set<AntPattern> antPatterns) {
			super();
			this.antPatterns = antPatterns;
		}

		@Override
		public boolean accept(String relativePath) {

			for (final AntPattern antPattern : antPatterns) {

				final boolean match = !antPattern.doMatch(relativePath);
				if (match) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "excludes " + antPatterns;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((antPatterns == null) ? 0 : antPatterns.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ExcludeFilter other = (ExcludeFilter) obj;
			if (antPatterns == null) {
				if (other.antPatterns != null) {
					return false;
				}
			} else if (!antPatterns.equals(other.antPatterns)) {
				return false;
			}
			return true;
		}

	}

	private static JakeFileFilter and(final JakeFileFilter filter1, final JakeFileFilter filter2) {

		return new JakeFileFilter() {

			@Override
			public boolean accept(String candidate) {
				return filter1.accept(candidate) && filter2.accept(candidate);
			}

			@Override
			public String toString() {
				return "{" + filter1 + " & " + filter2 + "}";
			}
		};
	}




}
