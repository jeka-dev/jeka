package dev.jeka.core.wrapper;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class BootLocator {

    private final static String JK_USER_HOM_ENV_NAME = "JEKA_USER_HOME";

    private final static String JK_WRAPPER_CACHE_ENV_NAME = "JEKA_WRAPPER_VERSIONS";

    private final static String BIN_NAME = "dev.jeka.jeka-core.jar";

    public static Path getJekaUserHomeDir() {
        final Path result;
        final String env = System.getenv(JK_USER_HOM_ENV_NAME);
        if (env != null && !env.trim().isEmpty()) {
            result = Paths.get(env);
        } else {
            result = Paths.get(System.getProperty("user.home")).resolve(".jeka");
        }
        if (Files.exists(result) && Files.isRegularFile(result)) {
            try {
                Files.delete(result);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }


    static Path getJekaVersionCacheDir(String verion) {
        final String cacheDir = System.getenv(JK_WRAPPER_CACHE_ENV_NAME);
        final Path result;
        if (cacheDir != null && !cacheDir.trim().isEmpty()) {
            result = Paths.get(cacheDir);
        } else {
            result = getJekaUserHomeDir().resolve("cache/wrapper/" + verion);
        }
        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    static Path getJekaBinPath(String version) {
        return getJekaVersionCacheDir(version).resolve(BIN_NAME);
    }

    static Path getWrapperPropsFile() {
        return Paths.get("jeka/boot/jeka.properties");
    }



}
