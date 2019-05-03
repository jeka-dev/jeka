package org.jerkar.api.java.junit;

import org.jerkar.api.file.JkPathMatcher;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.java.JkClasspath;

/**
 * Defines the tests classes to run along the classpath.
 *
 * @author Jerome Angibaud
 */
public final class JkJavaTestClasses {

    private final JkClasspath classpath;

    private final JkPathTreeSet classesToTest;

    public static JkJavaTestClasses of(JkClasspath classpath, JkPathTreeSet testClasses) {
        return new JkJavaTestClasses(classpath, testClasses);
    }

    public static JkJavaTestClasses of(JkClasspath classpath, JkPathTree testClasses) {
        return of(classpath, testClasses.toSet());
    }

    private JkJavaTestClasses(JkClasspath classpath, JkPathTreeSet testClasses) {
        this.classpath = classpath;
        this.classesToTest = testClasses;
    }

    /**
     * Returns the classpath to run these tests.
     */
    public JkClasspath getClasspath() {
        return classpath;
    }

    public JkPathTreeSet getClassesToTest() {
        return classesToTest;
    }

    public JkJavaTestClasses withClassesToTest(JkPathTreeSet classesToTest) {
        return new JkJavaTestClasses(this.classpath, classesToTest);
    }

    public JkJavaTestClasses withClassesToTest(String includePattern) {
        return withClassesToTest(this.classesToTest.withMatcher(JkPathMatcher.of(true, includePattern)));
    }



}
