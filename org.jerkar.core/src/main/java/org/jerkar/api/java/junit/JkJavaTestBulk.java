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
public final class JkJavaTestBulk {

    private final JkClasspath classpath;

    private final JkPathTreeSet classesToTest;

    public static JkJavaTestBulk of(JkClasspath classpath, JkPathTreeSet testClasses) {
        return new JkJavaTestBulk(classpath, testClasses);
    }

    public static JkJavaTestBulk of(JkClasspath classpath, JkPathTree testClasses) {
        return of(classpath, testClasses.toSet());
    }

    private JkJavaTestBulk(JkClasspath classpath, JkPathTreeSet testClasses) {
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

    public JkJavaTestBulk withClassesToTest(JkPathTreeSet classesToTest) {
        return new JkJavaTestBulk(this.classpath, classesToTest);
    }

    public JkJavaTestBulk withClassesToTest(String includePattern) {
        return withClassesToTest(this.classesToTest.withMatcher(JkPathMatcher.ofAccept(includePattern)));
    }



}
