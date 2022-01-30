package dev.jeka.core.api.file;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class JkUrlFileProxy {

    private final URL url;

    private final Path file;

    private JkUrlFileProxy(URL url, Path file) {
        this.url = url;
        this.file = file;
    }

    public static JkUrlFileProxy of(String url, Path path) {
        return new JkUrlFileProxy(JkUtilsIO.toUrl(url), path);
    }

    public static JkUrlFileProxy of(String url) {
        String filename = JkUtilsString.substringAfterLast(url, "/");
        Path file = JkLocator.getCachedUrlContentDir().resolve(filename);
        return of(url, file);
    }

    public Path get() {
        if (Files.exists(file)) {
            return file;
        }
        JkLog.info("Download " + url + " to " + file);
        return JkPathFile.of(file).createIfNotExist().fetchContentFrom(url).get();
    }

}
