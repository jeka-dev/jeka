package dev.jeka.core.tool;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Arrays;

public class PicocliMain {

    public static void main(String... args) throws Exception {

        // Jeka needs to run in with a specific class loader
        if (!(Thread.currentThread().getContextClassLoader() instanceof AppendableUrlClassloader)) {
            final URLClassLoader urlClassLoader = new AppendableUrlClassloader();
            Thread.currentThread().setContextClassLoader(urlClassLoader);
        }

        // Use reflection to avoid loading Delegate class twice
        Class<?> delegate = Thread.currentThread().getContextClassLoader()
                .loadClass("dev.jeka.core.tool.PicocliMainDelegate");
        Method method = delegate.getDeclaredMethod("doMain", String[].class);
        method.invoke(null, (Object) args);

    }


}
