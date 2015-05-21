package org.jerkar.builtins.eclipse;

import org.jerkar.file.JkFileTreeSet;

class Sources {

	public static final TestSegregator ALL_PROD = new NoTests();

	public static final TestSegregator SMART = new TestSegregator() {

		@Override
		public boolean isTest(String path) {
			return path.toLowerCase().contains("test");
		}
	};


	public final JkFileTreeSet prodSources;

	public final JkFileTreeSet testSources;

	public Sources(JkFileTreeSet prodSources, JkFileTreeSet testSources) {
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
