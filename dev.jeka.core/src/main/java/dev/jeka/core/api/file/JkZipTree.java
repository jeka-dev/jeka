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

import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * A {@link JkPathTree} for zip path tree located in a zip file.
 *  Instances are supposed to be closed by the user code, in a <i>try-with-resource</i> statement or
 *  in a <i>finally</i> clause.
 */
public class JkZipTree extends JkAbstractPathTree<JkZipTree> implements Closeable {

    private JkZipTree(JkUtilsPath.JkZipRoot zipRoot, JkPathMatcher pathMatcher) {
        super(zipRoot, pathMatcher);
    }

    /**
     * Creates a path tree from a zip file. The zip file content will be seen as a regular folder.
     * Warn : Don't forget to close this resource when you are finished with.
     */
    public static JkZipTree of(Path zipFile) {
        return new JkZipTree(JkUtilsPath.zipRoot(zipFile), JkPathMatcher.ACCEPT_ALL);
    }

    @Override
    protected JkZipTree newInstance(Supplier<Path> supplier, JkPathMatcher pathMatcher) {
        return new JkZipTree((JkUtilsPath.JkZipRoot) supplier, pathMatcher);
    }

    @Override
    protected JkZipTree withRoot(Path newRoot) {
        return newInstance(zipRoot().withRootInsideZip(newRoot), this.getMatcher());
    }

    @Override
    public JkZipTree createIfNotExist() {
        JkUtilsPath.JkZipRoot zipRoot = zipRoot();
        if (!Files.exists(zipRoot.getZipFile())) {
            JkUtilsPath.createDirectories(zipRoot.get().getParent());
        }
        if (!Files.exists(zipRoot.get())) {
            JkUtilsPath.createDirectories(zipRoot.get());
        }
        return this;
    }

    @Override
    public void close() {
        zipRoot().close();
    }

    private JkUtilsPath.JkZipRoot zipRoot() {
        return  (JkUtilsPath.JkZipRoot) this.rootSupplier;
    }

}
