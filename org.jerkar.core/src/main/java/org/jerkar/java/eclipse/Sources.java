package org.jerkar.java.eclipse;

import org.jerkar.JkDirSet;

class Sources {

	public static final TestSegregator ALL_PROD = new NoTests();

	public static final TestSegregator SMART = new TestSegregator() {

		@Override
		public boolean isTest(String path) {
			return path.toLowerCase().contains("test");
		}
	};


	public final JkDirSet prodSources;

	public final JkDirSet testSources;

	public Sources(JkDirSet prodSources, JkDirSet testSources) {
		super();
		this.prodSources = prodSources;
		this.testSources = testSources;
	}

	public static interface TestSegregator {

		public boolean isTest(String path);

	}

	private static class NoTests implements TestSegregator {

		@Override
		public boolean isTest(String path) {
			return false;
		}

	}

}
