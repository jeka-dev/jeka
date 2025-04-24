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

package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A dependency on files located on file system.
 */
public final class JkFileSystemDependency implements JkFileDependency {

    private final Path ideProjectDir;

    private JkFileSystemDependency(Iterable<Path> files, Path ideProjectDir) {
        this.files = Collections.unmodifiableList(JkUtilsIterable.listWithoutDuplicateOf(files));
        this.ideProjectDir = ideProjectDir;
    }

    /**
     * Creates a {@link JkFileSystemDependency} on the specified files.
     */
    public static JkFileSystemDependency of(Iterable<Path> files) {
        final Iterable<Path> trueFiles = JkUtilsPath.disambiguate(files);
        return new JkFileSystemDependency(trueFiles, null);
    }

    private final List<Path> files;

    @Override
    public List<Path> getFiles() {
        return files;
    }

    public JkFileSystemDependency minusFile(Path file) {
        List<Path> result = new LinkedList<>(files);
        result.remove(file);
        return new JkFileSystemDependency(result, ideProjectDir);
    }

    @Override
    public String toString() {
        if (files.size() == 1) {
            return files.get(0).toString();
        }
        return "Files=" + files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JkFileSystemDependency that = (JkFileSystemDependency) o;
        return files.equals(that.files);
    }

    @Override
    public int hashCode() {
        return files.hashCode();
    }

    @Override
    public Path getIdeProjectDir() {
        return ideProjectDir;
    }

    @Override
    public JkFileSystemDependency withIdeProjectDir(Path path) {
        return new JkFileSystemDependency(files, path);
    }
}