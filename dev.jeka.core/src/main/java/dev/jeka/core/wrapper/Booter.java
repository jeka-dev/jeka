package dev.jeka.core.wrapper;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Booter {

    private static final String MAIN_CLASS_NAME = "dev.jeka.core.tool.Main";

    private final static String JK_USER_HOM_ENV_NAME = "JEKA_USER_HOME";

    private final static String BIN_NAME = "dev.jeka.jeka-core.jar";

    public static void main(String[] args) throws Exception {
        final Path jekawDir = Paths.get(args[0]);
        Properties props = props(jekawDir);
        Path jekaBinPath = location(props);  // First try to get it from explicit location
        if (jekaBinPath == null) {
            final String version = version(props);
            jekaBinPath = getJekaBinPath(version);
            if (!Files.exists(jekaBinPath)) {
                final Path zip = downloadDistribZip(props, version);
                final Path dir = getJekaVersionCacheDir(version);
                System.out.println("Unzip distribution to " + dir + " ...");
                Files.createDirectories(dir);
                unzip(zip, dir);
                Files.deleteIfExists(dir.resolve("options.properties"));
                Files.deleteIfExists(dir.resolve("system.properties"));
                Files.deleteIfExists(dir.resolve("jeka.bat"));
                Files.deleteIfExists(dir.resolve("jeka"));
                System.out.println("Jeka " + version + " installed in " + dir);
            }
        }
        final ClassLoader classLoader = new URLClassLoader(new URL[] {jekaBinPath.toUri().toURL()});
        Thread.currentThread().setContextClassLoader(classLoader);
        final Class<?> mainClass = classLoader.loadClass(MAIN_CLASS_NAME);
        final Method method = mainClass.getMethod("main", String[].class);
        final String[] actualArgs = args.length <= 1 ? new String[0]
                : Arrays.asList(args).subList(1, args.length).toArray(new String[0]);
        method.invoke(null, (Object) actualArgs);
    }

    private static Path downloadDistribZip(Properties properties, String version) {
        String repo = properties.getProperty("jeka.repo.url");
        if (repo != null && !repo.trim().isEmpty()) {
            if (!repo.trim().endsWith("/")) {
                repo = repo.trim() + "/";
            }
        } else {
            repo = "https://repo.maven.apache.org/maven2/";
        }
        final String urlString = repo + "dev/jeka/jeka-core/"
                + version + "/jeka-core-" + version + "-distrib.zip";
        System.out.println("Downloading " + urlString + " ...");
        try {
            final URL url = new URL(urlString);
            ReadableByteChannel readableByteChannel = null;
            try {
                readableByteChannel = Channels.newChannel(url.openStream());
            } catch (final FileNotFoundException e) {
                System.out.println(urlString + " not found. Please check that version " + version + " exists in repo " + repo);
                System.out.println("Jeka version to download is defined in ./jeka/wrapper/jeka.properties file.");
                System.exit(1);
            }
            final Path temp = Files.createTempFile("jeka-wrapper", ".zip");
            try (FileOutputStream fileOutputStream = new FileOutputStream(temp.toFile())) {
                final FileChannel fileChannel = fileOutputStream.getChannel();
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            return temp;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void unzip(Path zip, Path dir) throws IOException {
        try (InputStream fis = Files.newInputStream(zip); ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                final Path file = dir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    if (Files.exists(file) && !Files.isDirectory(file)) {
                        Files.delete(file);
                    }
                    if (!Files.exists(file)){
                        Files.createDirectories(file);
                    }
                } else {
                    final Path parent = file.getParent();
                    if (!Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    if (Files.exists(file)) {
                        Files.delete(file);
                    }
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
                        final byte[] buffer = new byte[Math.toIntExact(entry.getSize())];
                        int location;
                        while ((location = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, location);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
        }
    }

    private static Path getJekaUserHomeDir() {
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
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            Files.createDirectories(result);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static Path getJekaVersionCacheDir(String verion) {
        final Path result = getJekaUserHomeDir().resolve("cache/wrapper/" + verion);
        try {
            Files.createDirectories(result);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static Path getJekaBinPath(String version) {
        return getJekaVersionCacheDir(version).resolve(BIN_NAME);
    }

    private static Path getWrapperPropsFile(Path jekawDir) {
        return jekawDir.resolve("jeka/wrapper/jeka.properties");
    }

    private static String version(Properties props) {
        final String result = props.getProperty("jeka.version");
        if (result == null || result.trim().isEmpty()) {
            System.out.println("Please, specify a jeka.version property in file ./jeka/wrapper/jeka.properties");
            System.exit(1);
        }
        return  result.trim();
    }

    private static Path location(Properties props) {
        final String result = props.getProperty("jeka.distrib.location");
        if (result == null || result.trim().isEmpty()) {
            return null;
        }
        return  Paths.get(result.trim()).resolve(BIN_NAME);
    }

    private static Properties props(Path jekawDir) {
        final Path propFile = getWrapperPropsFile(jekawDir);
        if (!Files.exists(propFile)) {
            System.out.println("No file found at " + propFile + ". Please rerun 'jeka scaffold#wrap");
            System.exit(1);
        }
        final Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(propFile)){
            props.load(inputStream);
            return props;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }



}
