package dev.jeka.core.tool;

import java.net.URL;
import java.net.URLClassLoader;

class RelaxedUrlClassloader extends URLClassLoader {

    RelaxedUrlClassloader() {
        super(new URL[] {}, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
