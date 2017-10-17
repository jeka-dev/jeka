package org.jerkar.api.java.junit;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

/**
 * Convenient class to launch Junit tests.
 *
 * @author Jerome Angibaud
 */
public final class JkJavaTestSpec {

    private final JkClasspath classpath;

    private final JkFileTreeSet classesToTest;

    public static JkJavaTestSpec of(JkClasspath classpath, JkFileTreeSet testClasses) {
        return new JkJavaTestSpec(classpath, testClasses);
    }

    public static JkJavaTestSpec of(JkClasspath classpath, JkFileTree testClasses) {
        return of(classpath, testClasses.asSet());
    }

    private JkJavaTestSpec(JkClasspath classpath, JkFileTreeSet testClasses) {
        this.classpath = classpath;
        this.classesToTest = testClasses;
    }

    /**
     * Returns the classpath for this launcher.
     */
    public JkClasspath classpath() {
        return classpath;
    }

    public JkFileTreeSet classesToTest() {
        return classesToTest;
    }

}
