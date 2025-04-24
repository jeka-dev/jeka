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

package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides location related to the running Jeka instance.
 *
 * @author Jerome Angibaud
 */
public final class JkLocator {

    private final static String JK_USER_HOME_ENV_NAME = "JEKA_USER_HOME";

    private final static String JK_CACHE_DIR_ENV_NAME = "JEKA_CACHE_DIR";

    private static Path JEKA_JAR_FILE;

    public static final String GLOBAL_PROPERTIES_FILENAME = "global.properties";

    /**
     * Returns the Jeka jar file currently used in the running process. Returns a folder if the classes
     * are not packaged in jar.
     */
    public static Path getJekaJarPath() {
        if (JEKA_JAR_FILE != null) {
            return JEKA_JAR_FILE;
        }
        Path result;
        try {
            URI uri = JkLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            result = Paths.get(uri);
        } catch (final Exception e) {
            try {
                URI uri = JkLocator.class.getResource('/' + JkLocator.class.getName()
                        .replace('.', '/') + ".class").toURI();
                String uriString = JkUtilsString.substringAfterFirst(uri.toString(), "jar:");
                uriString = JkUtilsString.substringBeforeFirst(uriString, "!");
                result = Paths.get(URI.create(uriString));
            } catch (URISyntaxException ex) {
                throw new IllegalStateException("Cannot find location of jeka jar", ex);
            }
        }
        JEKA_JAR_FILE = result;
        return result;
    }

    /**
     * Returns the directory where is installed the running Jeka instance.
     */
    public static Path getJekaHomeDir() {
        return getJekaJarPath().getParent();
    }

    /**
     * Returns the Jeka user directory.
     */
    public static Path getJekaUserHomeDir() {
        final Path result;
        final String env = System.getenv(JK_USER_HOME_ENV_NAME);
        if (!JkUtilsString.isBlank(env)) {
            result = Paths.get(env);
        } else {
            result = Paths.get(System.getProperty("user.home")).resolve(".jeka");
        }
        return ensureCreated(result);
    }

    public static Path getGlobalPropertiesFile() {
        return JkLocator.getJekaUserHomeDir().resolve(GLOBAL_PROPERTIES_FILENAME);
    }

    /**
     * Returns the location of the artifact repository cache.
     */
    public static Path getJekaRepositoryCache() {
        return ensureCreated(getCacheDir().resolve("repo"));
    }

    public static Path getCacheDir() {
        final Path result;
        final String env = System.getenv(JK_CACHE_DIR_ENV_NAME);
        if (!JkUtilsString.isBlank(env)) {
            result = Paths.get(env);
        } else {
            result = getJekaUserHomeDir().resolve("cache");
        }
        return ensureCreated(result);
    }

    public static Path getCachedUrlContentDir() {
        return ensureCreated(getCacheDir().resolve("url-content"));
    }

    /**
     * Determines whether the specified directory is a Jeka project.
     * A directory is considered a Jeka project if it contains a file named
     * "jeka.properties" or a subdirectory named "jeka-src".
     *
     * @param baseDir the base directory to check, which must not be null
     * @return true if the directory is identified as a Jeka project, false otherwise
     */
    public static boolean isJekaProject(Path baseDir) {
        if (!Files.isDirectory(baseDir)) {
            return false;
        }
        Path jekaPropertiesFile = baseDir.resolve(JkConstants.PROPERTIES_FILE);
        if (Files.exists(jekaPropertiesFile) && Files.isRegularFile(jekaPropertiesFile)) {
            return true;
        }
        Path jekaSrcDir = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        return Files.exists(jekaSrcDir) && Files.isDirectory(jekaSrcDir);
    }

    private static Path ensureCreated(Path path) {
        if (!Files.exists(path)) {
            JkUtilsPath.createDirectories(path);
        } else if (Files.isRegularFile(path)) {
            JkUtilsPath.deleteFile(path);
            JkUtilsPath.createDirectories(path);
        }
        return path;
    }


}
