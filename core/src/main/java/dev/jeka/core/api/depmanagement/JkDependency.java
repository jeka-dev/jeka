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

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkRunbase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Interface standing for a dependency. It can be either :
 * <ul>
 * <li>An external module as <code>org.hibernate:hibernate-core:3.0.1</code></li>
 * <li>A project inside a multi-project build</li>
 * <li>Some files on the file system</li>
 * <li>A process that produces 0 or n files</li>
 * </ul>
 *
 * @author Jerome Angibaud
 */
public interface JkDependency {

    /**
     * In the IDE, a dependency can be provided by a project exporting it. When generating IDE metadata files
     * (.classpath, xxx.iml), it's preferable to mention only the project exporting it rather than dependency
     * itself.
     * This method simply returns <code>null</code> when the dependency is not coming from another project.
     */
    Path getIdeProjectDir();

    /**
     * Returns a dependency identical to this one but with the specified project base dir.
     * @see #getIdeProjectDir()
     */
    JkDependency withIdeProjectDir(Path path);

    /**
     * Returns <code>true</code> if the specified dependency matches with this one. <p>
     * Matching means that two matching dependencies can not be declared in a same dependency set
     * as it will be considered as a duplicate or result in a conflict. <p>
     * For example "com.google:guava:21.0" is matching with "com.google:guava:23.0" even if they are not equals.
     */
    default boolean matches(JkDependency other) {
        return this.equals(other);
    }

    /**
     * Returns either a {@link JkCoordinateDependency} or a {@link JkFileSystemDependency}
     * depending on the specified parameter stands for a path or a coordinate.
     */
    static JkDependency of(String pathOrCoordinate) {
        if (isCoordinate(pathOrCoordinate)) {
            return JkCoordinateDependency.of(pathOrCoordinate.trim());
        }
        return JkFileSystemDependency.of(Paths.get(pathOrCoordinate));
    }

    /**
     * Same as {@link #of(String)} but resolve relative paths against the specified base directory.
     */
    static JkDependency of(Path baseDir, String pathOrCoordinate) {
        if (isCoordinate(pathOrCoordinate)) {
            return JkCoordinateDependency.of(pathOrCoordinate.trim());
        }
        Path path = Paths.get(pathOrCoordinate);
        path =  path.isAbsolute() ? path : baseDir.resolve(path);
        return JkFileSystemDependency.of(path);
    }

    /**
     * Determines whether the given String represents a coordinate or a file path.
     */
    static boolean isCoordinate(String pathOrCoordinate) {
        boolean hasColon = pathOrCoordinate.contains(":");
        boolean windowsFile = JkUtilsSystem.IS_WINDOWS &&
                ( pathOrCoordinate.startsWith(":\\", 1) || pathOrCoordinate.startsWith(":/", 1) );
        return hasColon && !windowsFile;
    }


}
