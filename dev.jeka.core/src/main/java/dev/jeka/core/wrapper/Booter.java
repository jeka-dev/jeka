package dev.jeka.core.wrapper;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/* This class must not depend on any other package in dev.jeka.core project as it will be turned in its own jar.

   This class is made public to be used by IDE tools.
 */
public class Booter {

    private static final String MAIN_CLASS_NAME = "dev.jeka.core.tool.Main";

    private static final String JK_USER_HOM_ENV_NAME = "JEKA_USER_HOME";

    private static final String BIN_NAME = "dev.jeka.jeka-core.jar";

    private static final String JK_CACHE_ENV_NAME = "JEKA_CACHE_DIR";

    public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";

    public static final String GITHUB_PACKAGE_URL = "https://maven.pkg.github.com/jeka-dev/jeka";

    public static void main(String[] args) throws Exception {
        final Path jekawDir = Paths.get(args[0]);
        Properties props = props(jekawDir);
        props.putAll(props(args));
        Path jekaBinPath = location(props);  // First try to get it from explicit location
        if (jekaBinPath == null) {
            final String version = version(props);
            jekaBinPath = getJekaBinPath(version);
            if (!Files.exists(jekaBinPath)) {
                String baseUrl = props.getProperty("jeka.distrib.repo", repoOptions());
                if (baseUrl == null) {
                    baseUrl = MAVEN_CENTRAL_URL;
                }
                final Path dir = install(baseUrl, version);
                Files.deleteIfExists(dir.resolve("global.properties"));
                Files.deleteIfExists(dir.resolve("jeka.bat"));
                Files.deleteIfExists(dir.resolve("jeka"));
                System.out.println("Jeka " + version + " installed in " + dir);
            }
        } else if (!Files.exists(jekaBinPath)) {
            System.out.println("File " + jekaBinPath + " mentioned in property file not found");
            System.exit(1);
        }
        List<URL> classpath = new LinkedList<>();
        classpath.addAll(getBootLibs());
        classpath.add(jekaBinPath.toUri().toURL());
        final ClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[0]));
        Thread.currentThread().setContextClassLoader(classLoader);
        final Class<?> mainClass = classLoader.loadClass(MAIN_CLASS_NAME);
        final Method method = mainClass.getMethod("main", String[].class);
        final String[] actualArgs = args.length <= 1 ? new String[0]
                : Arrays.copyOfRange(args, 1, args.length);
        method.invoke(null, (Object) actualArgs);
    }

    /**
     * Download and install the jeka distribution of the specified version
     * @return The directory of the newly installed distribution
     */
    public static Path install(String baseUrl, String version) {
        final Path zip = downloadDistribZip(baseUrl, version);
        final Path dir = getJekaVersionCacheDir(version);
        System.out.println("Unzip distribution to " + dir + " ...");
        try {
            Files.createDirectories(dir);
            unzip(zip, dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir;
    }

    private static List<URL> getBootLibs() {
        Path bootDir = Paths.get("jeka/boot");
        if (!Files.exists(bootDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(bootDir)){
            return stream
                    .filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .map(path -> {
                        try {
                            return path.toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path downloadDistribZip(String baseUrl, String version) {
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        final String urlString = base + "dev/jeka/jeka-core/"
                + version + "/jeka-core-" + version + "-distrib.zip";
        System.out.println("Downloading " + urlString + " ...");
        final URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream())) {
            final Path temp = Files.createTempFile("jeka-wrapper", ".zip");
            try (FileOutputStream fileOutputStream = new FileOutputStream(temp.toFile())) {
                final FileChannel fileChannel = fileOutputStream.getChannel();
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            return temp;
        } catch (final FileNotFoundException e) {
            System.out.println(urlString + " not found. Please check that version " + version + " exists in repo " + baseUrl);
            System.out.println("Jeka version to download is defined in ./jeka/wrapper/wrapper.properties file.");
            System.exit(1);
            return null;
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

    private static Path getCacheDir() {
        final Path result;
        final String env = System.getenv(JK_CACHE_ENV_NAME);
        if (env != null && !env.trim().isEmpty()) {
            result = Paths.get(env);
        } else {
            result = getJekaUserHomeDir().resolve("cache");
        }
        return result;
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

    private static Path getJekaVersionCacheDir(String version) {
        final Path result = getCacheDir().resolve("distributions/" + version);
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
        return jekawDir.resolve("jeka/wrapper/wrapper.properties");
    }

    private static String version(Properties props) {
        final String result = props.getProperty("jeka.version");
        if (result == null || result.trim().isEmpty()) {
            System.out.println("Please, specify a jeka.version property in file ./jeka/wrapper/wrapper.properties");
            System.exit(1);
        }
        return  result.trim();
    }

    private static Path location(Properties props) {
        final String result = props.getProperty("jeka.distrib.location");
        if (result == null || result.trim().isEmpty()) {
            return null;
        }
        return Paths.get(result.trim()).resolve(BIN_NAME);
    }

    private static String repoOptions() {
        Properties properties = new Properties();
        Path globalPropertyFile = getJekaUserHomeDir().resolve("global.properties");
        if (!Files.exists(globalPropertyFile)) {
            return null;
        }
        try (InputStream inputStream = Files.newInputStream(globalPropertyFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String url= properties.getProperty("repo.download.url");
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return url.trim().endsWith("/") ? url.trim() : url.trim() + "/";
    }

    private static Properties props(Path jekawDir) {
        final Path propFile = getWrapperPropsFile(jekawDir);
        if (!Files.exists(propFile)) {
            System.out.println("No file found at " + propFile + ". Please re-run 'jeka scaffold#wrapper");
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

    private static Properties props(String[] args) {
        Properties props = new Properties();
        Arrays.stream(args)
                .filter(arg -> arg.startsWith("-D"))
                .map(arg -> arg.substring(2))
                .filter(arg -> arg.contains("="))
                .map(arg -> arg.split("="))
                .forEach(items -> props.put(items[0], items[1]));
        return props;
    }

}
