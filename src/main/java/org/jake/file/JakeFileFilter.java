package org.jake.file;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.jake.file.utils.JakeUtilsFile;

public abstract class JakeFileFilter {
	
	public static final JakeFileFilter ACCEPT_ALL = new JakeFileFilter() {

		@Override
		public boolean accept(String relativePath) {
			return true;
		}
		
	};
	
	public abstract boolean accept(String relativePath);
	
	public static JakeFileFilter include(String ... antPatterns) {
		return new IncludeFilter(AntPattern.arrayOf(antPatterns));
	}
	
	public static JakeFileFilter exclude(String ... antPatterns) {
		return new ExcludeFilter(AntPattern.arrayOf(antPatterns));
	}
		
	public JakeFileFilter andIncludeOnly(String ... antPatterns) {
		return this.and(include(antPatterns));
	}
	
	public JakeFileFilter andExcludeAll(String ... antPatterns) {
		return this.and(exclude(antPatterns));
	}
	
	public JakeFileFilter and(JakeFileFilter other) {
		return and(this, other);
	}
	
	public JakeFileFilter or(JakeFileFilter other) {
		return or(this, other);
	}
	
	public JakeFileFilter reverse() {
		return new JakeFileFilter() {

			@Override
			public boolean accept(String relativePath) {
				return !JakeFileFilter.this.accept(relativePath);
			}
			
		};
	}
	
	public FileFilter toFileFilter(final File baseDir) {
		return new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				final String relativePath = JakeUtilsFile.getRelativePath(baseDir, file);
				return JakeFileFilter.this.accept(relativePath);
			}
		};
	}
	
	
	private static class IncludeFilter extends JakeFileFilter {
		
		private final AntPattern[] antPatterns;
		
		public IncludeFilter(AntPattern[] antPatterns) {
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
			return "includes" + Arrays.toString(antPatterns);
		}
	} 
	
	private static class ExcludeFilter extends JakeFileFilter {
		
		private final AntPattern[] antPatterns;
		
		public ExcludeFilter(AntPattern[] antPatterns) {
			super();
			this.antPatterns = antPatterns;
		}

		@Override
		public boolean accept(String relativePath) {
			
			for (final AntPattern antPattern : antPatterns) {
				
				boolean match = !antPattern.doMatch(relativePath);
				if (match) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "excludes" + Arrays.toString(antPatterns);
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
	
	private static JakeFileFilter or(final JakeFileFilter filter1, final JakeFileFilter filter2) {
		
		return new JakeFileFilter() {

			@Override
			public boolean accept(String candidate) {
				return filter1.accept(candidate) || filter2.accept(candidate);
			}

			@Override
			public String toString() {
				return "{" + filter1 + " | " + filter2 + "}";
			}
		};
	}

	
}
