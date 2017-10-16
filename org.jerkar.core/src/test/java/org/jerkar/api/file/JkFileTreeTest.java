package org.jerkar.api.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.utils.JKUtilsTests;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsPathTest;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkFileTreeTest {

    @Test
    public void testFilesOnly() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final Path sampleFile = Paths.get(sampleFileUrl.toURI());
        assertTrue(Files.exists(sampleFile));
        final Path sampleFolder = sampleFile.getParent().getParent();

        System.out.println(JkFileTree.of(sampleFolder).filesOnlyRelative());

        final JkFileTree subfolderTxt1 = JkFileTree.of(sampleFolder).include("/subfolder/*.txt");
        assertEquals(1, subfolderTxt1.filesOnly().size());
        System.out.println(subfolderTxt1);

        final JkFileTree subfolderTxt2 = JkFileTree.of(sampleFolder).include("subfolder/*.txt");
        assertEquals(1, subfolderTxt2.filesOnly().size());
    }

    @Test
    public void testRelativeFiles() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final JkFileTree tree = JkFileTree.of(Paths.get(sampleFileUrl.toURI()).getParent().getParent());
        System.out.println(tree.filesOnlyRelative());
    }

    @Test
    public void testZipTo() throws Exception {
        Path zip = Files.createTempFile("filetree", ".zip");
        Files.delete(zip);
        JkFileTree.of(sampleDir()).zipTo(zip);
        Path zipRoot = JkUtilsPath.zipRoot(zip);
        assertTrue(Files.exists(zipRoot.resolve("subfolder/sample.txt")));
        zipRoot.getFileSystem().close();

        // Test overwrite
        JkFileTree.of(sampleDir()).zipTo(zip);
    }

    @Test
    public void testOfZip() throws Exception {
        Path zipFile = Paths.get("toto.zip");
        Files.deleteIfExists(zipFile);
        JkFileTree zipTree = JkFileTree.ofZip(zipFile);
        zipTree.importContent(sampleDir());
        List<Path> paths = zipTree.filesOnly();
        assertEquals(1, paths.size());

        //System.out.println(Files.exists(zipRoot));
        //zipRoot.getFileSystem().close();
        Files.delete(zipFile);
    }

    @Test  // Ensure we can create a zip from a zip
    public void testZipZipTo() throws IOException, URISyntaxException {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkFileTree.ofZip(zip).zipTo(zip2);
        JkFileTree zip2Tree = JkFileTree.ofZip(zip2);
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test  // Ensure we can import  a zip from a zip
    public void testZipImportZipContentZipTo() throws IOException, URISyntaxException {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkFileTree zip2Tree = JkFileTree.ofZip(zip2);
        zip2Tree.importContent(JkFileTree.ofZip(zip).root());
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }


    private static Path sampleDir() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir
        final Path sampleFile = Paths.get(sampleFileUrl.toURI());
        assertTrue(Files.exists(sampleFile));
        return sampleFile.getParent().getParent();
    }

    private static Path sampleFolder() throws IOException, URISyntaxException {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir
        final Path sampleFile = Paths.get(sampleFileUrl.toURI());
        assertTrue(Files.exists(sampleFile));
        return sampleFile.getParent().getParent();
    }

    private static Path createSampleZip() throws IOException, URISyntaxException {
        Path folder = sampleFolder();
        Path zip = Files.createTempFile("sample", ".zip");
        Files.delete(zip);
        JkFileTree.of(folder).zipTo(zip);
        return zip;
    }


}
