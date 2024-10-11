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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkZipTree;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Low level utility method to deal with zip files.
 */
public final class JkUtilsZip {

    private JkUtilsZip() {
    }

    /**
     * Returns all zip entry of the specified zip file.
     */
    @SuppressWarnings("unchecked")
    public static List<ZipEntry> getZipEntries(ZipFile zipFile) {
        final List<ZipEntry> result = new LinkedList<>();
        final Enumeration<ZipEntry> en = (Enumeration<ZipEntry>) zipFile.entries();
        while (en.hasMoreElements()) {
            result.add(en.nextElement());
        }
        return result;
    }

    /**
     * Creates a {@link ZipFile} to file without checked exception.
     */
    public static ZipFile getZipFile(File file) {
        try {
            return new ZipFile(file);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    public static JarFile jarFile(Path path) {
        try {
            return new JarFile(path.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Unzips the contents of a URL to a specified target path.
     *
     * @param url The URL of the zip file to be unzipped.
     * @param zipSubPath The subpath within the zip file to be unzipped. Pass null or empty string to unzip the entire zip file.
     * @param target The destination path where the unzipped files will be copied to.
     */
    public static void unzipUrl(String url, String zipSubPath, Path target) {
        Path temp = JkPathFile.ofTemp("jk-unzip", ".zip").fetchContentFrom(url).get();
        try (JkZipTree zip = JkZipTree.of(temp)) {
            zip.goTo(zipSubPath).copyTo(target, StandardCopyOption.REPLACE_EXISTING);
        }
        JkUtilsPath.deleteFile(temp);
    }

    public static void unzip(Path zipFile, Path target) {
        try (JkZipTree zip = JkZipTree.of(zipFile)) {
            zip.copyTo(target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
