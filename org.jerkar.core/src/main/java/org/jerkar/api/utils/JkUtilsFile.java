package org.jerkar.api.utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for dealing with files.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsFile {

    /**
     * Returns the content of the specified property file as a
     * {@link Properties} object.
     */
    private static Properties readPropertyFile(Path propertyfile) {
        final Properties props = new Properties();
        try (InputStream fileInputStream = Files.newInputStream(propertyfile)){
            props.load(fileInputStream);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        return props;
    }

    public static Map<String, String> readPropertyFileAsMap(Path propertyfile) {
        final Properties properties = readPropertyFile(propertyfile);
        return JkUtilsIterable.propertiesToMap(properties);
    }

    /**
     * Get the url to the specified file.
     */
    public static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void zip(Path folderOrFile, Path zipFile) {
        try (OutputStream fos = Files.newOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {
            zipFolder(folderOrFile, null, zipFile, zipOut);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }


    private static void zipFolder(Path folderOrFile, String fileName, Path zipFile, ZipOutputStream zipOut) throws IOException {
        if(Files.isDirectory(folderOrFile)) {
            List<Path> children = Files.list(folderOrFile).collect(Collectors.toList());
            for (Path childFile : children) {
                String entryName = fileName == null ? childFile.getFileName().toString() : fileName + "/" + childFile.getFileName();
                zipFolder(childFile, entryName, zipFile, zipOut);
            }
            return;
        }

        InputStream fis = Files.newInputStream(folderOrFile);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();

    }

}
