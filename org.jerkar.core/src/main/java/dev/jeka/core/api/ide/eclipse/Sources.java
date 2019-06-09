package dev.jeka.core.api.ide.eclipse;

import dev.jeka.core.api.file.JkPathTreeSet;

class Sources {

    public static final TestSegregator ALL_PROD = new NoTests();

    public static final TestSegregator SMART = path -> path.toLowerCase().contains("test");

    public final JkPathTreeSet prodSources;

    public final JkPathTreeSet testSources;

    public Sources(JkPathTreeSet prodSources, JkPathTreeSet testSources) {
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
