package org.jerkar.api.java.junit;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.java.JkClasspath;

/**
 * Convenient class to launch Junit tests.
 *
 * @author Jerome Angibaud
 */
public final class JkJavaTestSpec {

    private final JkClasspath classpath;

    private final JkPathTreeSet classesToTest;

    public static JkJavaTestSpec of(JkClasspath classpath, JkPathTreeSet testClasses) {
        return new JkJavaTestSpec(classpath, testClasses);
    }

    public static JkJavaTestSpec of(JkClasspath classpath, JkPathTree testClasses) {
        return of(classpath, testClasses.asSet());
    }

    private JkJavaTestSpec(JkClasspath classpath, JkPathTreeSet testClasses) {
        this.classpath = classpath;
        this.classesToTest = testClasses;
    }

    /**
     * Returns the classpath for this launcher.
     */
    public JkClasspath classpath() {
        return classpath;
    }

    public JkPathTreeSet classesToTest() {
        return classesToTest;
    }

}
