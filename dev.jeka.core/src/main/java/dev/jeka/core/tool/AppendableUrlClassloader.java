package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

class AppendableUrlClassloader extends URLClassLoader {

    AppendableUrlClassloader() {
        super(new URL[] {}, Thread.currentThread().getContextClassLoader());
    }

    private void addEntry(Iterable<Path> path) {
        List<Path> paths = JkUtilsPath.disambiguate(path);
        paths.forEach(aPath -> addURL(JkUtilsPath.toUrl(aPath.toAbsolutePath().normalize())));
    }

    static void addEntriesOnContextClassLoader(Iterable<Path> paths) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof AppendableUrlClassloader) {
            ((AppendableUrlClassloader) classLoader).addEntry(paths);
        } else {
            JkUrlClassLoader.ofCurrent().addEntries(paths);
        }
    }

    @Override
    public String toString() {
        return "Jeka ClassLoader : " + AppendableUrlClassloader.class.getName();
    }
}
