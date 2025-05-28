/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.file;

import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class JkUrlFileProxy {

    private final URL url;

    private final Path file;

    private Consumer<HttpURLConnection> urlConnectionCustomizer = urlConnection -> {};

    private JkUrlFileProxy(URL url, Path file) {
        this.url = url;
        this.file = file;
    }

    public static JkUrlFileProxy of(String url, Path path) {
        return new JkUrlFileProxy(JkUtilsIO.toUrl(url), path);
    }

    public JkUrlFileProxy setUrlConnectionCustomizer(Consumer<HttpURLConnection> urlConnectionCustomizer) {
        this.urlConnectionCustomizer = urlConnectionCustomizer;
        return this;
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
        JkHttpRequest.of(url.toString())
                .customize(urlConnectionCustomizer)
                .downloadFile(file);
        return JkPathFile.of(file).createIfNotExist().fetchContentFrom(url).get();
    }

}
