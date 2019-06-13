package dev.jeka.core.api.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

import dev.jeka.core.api.file.JkPathMatcher;
import org.junit.Test;


@SuppressWarnings("javadoc")
public class JkUtilsPathTest {

    @Test
    public void testCopyDir() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir
        final Path target = Files.createTempDirectory("copydirtest");
        JkUtilsPath.copyDirContent(source, target, path -> true, StandardCopyOption.REPLACE_EXISTING);
        System.out.println(target);
        assertTrue(Files.exists(target.resolve("subfolder/sample.txt")));
        assertTrue(Files.exists(target.resolve("emptyfolder")));
        assertTrue(Files.isDirectory(target.resolve("emptyfolder")));

        final Path subfolder = Paths.get(sampleFileUrl.toURI()).getParent();
        final Path target2 = Files.createTempDirectory("copydirtest");
        JkUtilsPath.copyDirContent(subfolder, target2, path -> true, StandardCopyOption.REPLACE_EXISTING);
        System.out.println(target2);
        assertTrue(Files.exists(target2.resolve("sample.txt")));

        // Test copy with matcher
        final Path target3 = Files.createTempDirectory("copydirtest");
        PathMatcher pathMatcher = JkPathMatcher.of(false, "subfolder/**");
        JkUtilsPath.copyDirContent(source, target3, pathMatcher, StandardCopyOption.REPLACE_EXISTING);
        assertFalse(Files.exists(target3.resolve("subfolder/sample.txt")));

        final Path target4 = Files.createTempDirectory("copydirtest");
        pathMatcher = JkPathMatcher.of(false, "subfolder/**", "subfolder");
        JkUtilsPath.copyDirContent(source, target4, pathMatcher, StandardCopyOption.REPLACE_EXISTING);
        assertFalse(Files.exists(target4.resolve("subfolder")));
    }

    @Test
    public void testZipRoot() throws IOException {
        Path zipFile = Paths.get("toto.zip");
        Files.deleteIfExists(zipFile);
        Path zipPath = JkUtilsPath.zipRoot(zipFile);  // create the zip file if needed
        assertTrue(Files.exists(zipPath));
        Files.deleteIfExists(zipFile);
        Path dirWithSpaces = Files.createTempDirectory("folder name with space");
        Path otherPath = dirWithSpaces.resolve("toto.zip");
        System.out.println(otherPath.toUri());
        JkUtilsPath.zipRoot(otherPath);
    }


}
