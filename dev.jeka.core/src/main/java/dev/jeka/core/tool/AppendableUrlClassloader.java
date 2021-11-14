package dev.jeka.core.tool;

import java.net.URL;
import java.net.URLClassLoader;

class AppendableUrlClassloader extends URLClassLoader {

    AppendableUrlClassloader() {
        super(new URL[] {}, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
