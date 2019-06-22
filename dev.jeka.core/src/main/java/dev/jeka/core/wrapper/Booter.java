package dev.jeka.core.wrapper;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Booter {

    private static final String MAIN_CLASS_NAME = "dev.jeka.core.tool.Main";

    public static void main(String[] args) throws Exception {
        String version = version();
        Path path = BootLocator.getJekaBinPath(version);
        if (!Files.exists(path)) {
            Path zip = downloadDistribZip(version);
            Path dir = BootLocator.getJekaVersionCacheDir(version);
            System.out.println("Unzip distribution to " + dir + " ...");
            Files.createDirectories(dir);
            unzip(zip, dir);
            Files.delete(zip);
            System.out.println("Jeka " + version + " installed in " + dir);
        }
        ClassLoader classLoader = new URLClassLoader(new URL[] {path.toUri().toURL()});
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> mainClass = classLoader.loadClass(MAIN_CLASS_NAME);
        Method method = mainClass.getMethod("main", String[].class);
        method.invoke(null, (Object) args);
    }

    private static String version() {
        Path propFile = BootLocator.getWrapperPropsFile();
        if (!Files.exists(propFile)) {
            System.out.println("No file found at " + propFile + ". Please rerun 'jeka scaffold#wrap");
            System.exit(1);
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(propFile.toFile()));
            String result = props.getProperty("jeka.version");
            if (result == null || result.trim().isEmpty()) {
                System.out.println("Please, specify a jeka.version property in file ./jeka/boot/jeka.properties");
                System.exit(1);
            }
            return  result.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path downloadDistribZip(String version) {
        String repo = "https://repo.maven.apache.org/maven2";
        String urlString = repo + "/dev/jeka/jeka-core/"
                + version + "/jeka-core-" + version + "-distrib.zip";
        System.out.println("Downloading " + urlString + " ...");
        try {
            URL url = new URL(urlString);
            ReadableByteChannel readableByteChannel = null;
            try {
                readableByteChannel = Channels.newChannel(url.openStream());
            } catch (FileNotFoundException e) {
                System.out.println(urlString + " not found. Please check that version " + version + " exists in repo " + repo);
                System.out.println("Jeka version to download is defined in ./jeka/boot/jeka.properties file.");
                System.exit(1);
            }
            Path temp = Files.createTempFile("jeka-wrapper", ".zip");
            FileOutputStream fileOutputStream = new FileOutputStream(temp.toFile());
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return temp;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void unzip(Path zip, Path dir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip.toFile()))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                Path file = dir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    if (Files.exists(file) && !Files.isDirectory(file)) {
                        Files.delete(file);
                    }
                    if (!Files.exists(file)){
                        Files.createDirectories(file);
                    }
                } else {
                    Path parent = file.getParent();
                    if (!Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    if (Files.exists(file)) {
                        Files.delete(file);
                    }
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
                        byte[] buffer = new byte[Math.toIntExact(entry.getSize())];
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

}
