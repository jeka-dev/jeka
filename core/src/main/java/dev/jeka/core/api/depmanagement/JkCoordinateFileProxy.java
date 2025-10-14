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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 *  Class behaving as a file supplier for a given dependency.
 */
public final class JkCoordinateFileProxy {

    private final JkRepoSet repoSet;

    private final JkCoordinate coordinate;

    private JkCoordinateFileProxy(JkRepoSet repoSet, JkCoordinate coordinate) {
        this.repoSet = repoSet;
        this.coordinate = coordinate;
    }

    public static JkCoordinateFileProxy of(JkRepoSet repoSet, JkCoordinate coordinate) {
        return new JkCoordinateFileProxy(repoSet, coordinate);
    }

    public static JkCoordinateFileProxy of(JkRepoSet repoSet, @JkDepSuggest String coordinate) {
        return of(repoSet, JkCoordinate.of(coordinate));
    }

    public static JkCoordinateFileProxy ofStandardRepos(JkProperties properties, @JkDepSuggest String coordinate) {
        JkRepoSet repos = JkRepoProperties.of(properties).getDownloadRepos();
        return of(repos, coordinate);
    }

    public Path get() {
        Path result = coordinate.cachePath();
        if (!Files.exists(result)) {
            JkLog.verbose("File %s not found in cache.", result);
            JkLog.verbose("Download %s from repos %s", coordinate, repoSet);
            Path downloadPath = repoSet.get(coordinate);
            JkUtilsAssert.state(downloadPath != null, "Dependency %s not resolved", coordinate);
            JkUtilsAssert.state(result.equals(downloadPath),
                    "File %s computed for caching %s is different than download file %s. " +
                            "Check the cache path pattern is correct in cachePath() implementation.", result,
                    coordinate, downloadPath);
            return result;
        }
        return result;
    }

    public boolean exists() {
        Path result = coordinate.cachePath();
        return Files.exists(result) || repoSet.get(coordinate) != null;
    }

}
