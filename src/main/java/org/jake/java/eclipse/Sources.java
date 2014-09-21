package org.jake.java.eclipse;

import org.jake.JakeDirSet;

class Sources {

	public static final TestSegregator ALL_PROD = new NoTests();

	public static final TestSegregator SMART = new TestSegregator() {

		@Override
		public boolean isTest(String path) {
			return path.toLowerCase().contains("test");
		}
	};


	public final JakeDirSet prodSources;

	public final JakeDirSet testSources;

	public Sources(JakeDirSet prodSources, JakeDirSet testSources) {
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
