package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
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

    public static JkCoordinateFileProxy of(JkRepoSet repoSet, String dependencyDescription) {
        return of(repoSet, JkCoordinate.of(dependencyDescription));
    }

    public static JkCoordinateFileProxy ofStandardRepos(String dependencyDescription) {
        return of(JkRepoFromProperties.getDownloadRepos().and(JkRepo.ofMavenCentral()), dependencyDescription);
    }

    public Path get() {
        Path result = coordinate.cachePath();
        if (!Files.exists(result)) {
            JkLog.trace("File %s not found in cache.", result);
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


}
