package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;

class JkUnit5Handler {

    private static final String TEST_ANNOTATION_NAME = "org.junit.jupiter.api.Test";

    private static final String PLATFORM_LAUNCHER_CLASS_NAME = "org.junit.platform.launcher.Launcher";

    private final JkClasspath classpath;

    private final Path testClassRootPath;

    private JkUnit5Handler(JkClasspath classpath, Path testClassRootPath) {
        this.classpath = classpath;
        this.testClassRootPath = testClassRootPath;
    }

    public JkUnit5Handler of (JkClasspath classpath, Path testClassRootPath) {
        JkUtilsAssert.isTrue(classpath.entries().contains(testClassRootPath), "classpath " + classpath +
                " does not contains " + testClassRootPath);
        return new JkUnit5Handler(classpath, testClassRootPath);
    }

    static boolean isJupiterApiPresent(JkClassLoader classLoader) {
        return classLoader.isDefined(TEST_ANNOTATION_NAME);
    }

    public void launchInClassloader() {
        if (classpath.getEntryContainingClass(PLATFORM_LAUNCHER_CLASS_NAME) != null) {
            //JkUrlClassLoader.of(classpath, ClassLoader.getSystemClassLoader().getParent()).toJkClassLoader().
        }
    }





}
