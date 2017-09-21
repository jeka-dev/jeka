package org.jerkar.api.ide.eclipse;

import org.jerkar.api.file.JkFileTreeSet;

class Sources {

    public static final TestSegregator ALL_PROD = new NoTests();

    public static final TestSegregator SMART = path -> path.toLowerCase().contains("test");

    public final JkFileTreeSet prodSources;

    public final JkFileTreeSet testSources;

    public Sources(JkFileTreeSet prodSources, JkFileTreeSet testSources) {
        super();
        this.prodSources = prodSources;
        this.testSources = testSources;
    }

    public interface TestSegregator {

        boolean isTest(String path);

    }

    private static class NoTests implements TestSegregator {

        @Override
        public boolean isTest(String path) {
            return false;
        }

    }

}
