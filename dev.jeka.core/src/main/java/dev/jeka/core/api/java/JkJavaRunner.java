package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.util.jar.Attributes;

public class JkJavaRunner {

    public static void runInSeparateClassloader(Path jar, String ... args) {
        JkManifest manifest = JkManifest.of().loadFromJar(jar);
        String className = manifest.getMainAttribute(Attributes.Name.MAIN_CLASS);
        JkUtilsAssert.argument(className != null, "Jar " + jar.getParent()
                + " manifest does not contains Main-Class attribute.");
        Class<?> mainClass = JkUrlClassLoader.of(jar, ClassLoader.getSystemClassLoader()).toJkClassLoader()
                .load(className);
        JkUtilsReflect.invokeStaticMethod(mainClass, "main", (Object[]) args);
    }
}
