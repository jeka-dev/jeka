package org.jerkar.api.file;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsPathTest;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkPathTreeTest {

    @Test
    public void testFilesOnly() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final Path sampleFile = Paths.get(sampleFileUrl.toURI());
        assertTrue(Files.exists(sampleFile));
        final Path sampleFolder = sampleFile.getParent().getParent();

        System.out.println(JkPathTree.of(sampleFolder).getRelativeFiles());

        final JkPathTree subfolderTxt1 = JkPathTree.of(sampleFolder).andAccept("subfolder/*.txt");
        assertEquals(1, subfolderTxt1.getFiles().size());
        System.out.println(subfolderTxt1);

        final JkPathTree subfolderTxt2 = JkPathTree.of(sampleFolder).andAccept("subfolder/*.txt");
        assertEquals(1, subfolderTxt2.getFiles().size());
    }

    @Test
    public void testRelativeFiles() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final JkPathTree tree = JkPathTree.of(Paths.get(sampleFileUrl.toURI()).getParent().getParent());
        System.out.println(tree.getRelativeFiles());
    }

    @Test
    public void testStream() throws Exception {
        Path sampleDir = sampleDir();

        // Root is included in the getOutputStream
        assertTrue(JkPathTree.of(sampleDir).stream().anyMatch(path -> path.equals(sampleDir)));
    }

    @Test
    public void testZipTo() throws Exception {
        Path zip = Files.createTempFile("filetree", ".zip");
        Files.delete(zip);
        JkPathTree.of(sampleDir()).zipTo(zip);
        Path zipRoot = JkUtilsPath.zipRoot(zip);
        assertTrue(Files.exists(zipRoot.resolve("subfolder/sample.txt")));
        zipRoot.getFileSystem().close();

        // Test overwrite
        JkPathTree.of(sampleDir()).zipTo(zip);
    }

    @Test
    public void testOfZip() throws Exception {
        Path zipFile = Files.createTempFile("jksample",".zip");
        Files.deleteIfExists(zipFile);
        JkPathTree zipTree = JkPathTree.ofZip(zipFile);
        zipTree.merge(sampleDir());
        List<Path> paths = zipTree.getFiles();
        assertEquals(1, paths.size());

        //System.out.println(Files.exists(zipRoot));
        //zipRoot.getFileSystem().close();
        Files.delete(zipFile);
    }

    @Test  // Ensure we can create a zip from a zip
    public void testZipZipTo() throws Exception {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkPathTree.ofZip(zip).zipTo(zip2);
        JkPathTree zip2Tree = JkPathTree.ofZip(zip2);
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test  // Ensure we can import  a zip from a zip
    public void testZipMergeDir() throws Exception {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkPathTree zip2Tree = JkPathTree.ofZip(zip2);
        zip2Tree.merge(JkPathTree.ofZip(zip));
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test
    public void testImportTree() throws Exception {
        Path zip = createSampleZip();
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree tree = JkPathTree.of(dirSample);
        tree.merge(JkPathTree.ofZip(createSampleZip()));
        assertTrue(Files.isDirectory(tree.get("subfolder")));
        assertTrue(Files.isRegularFile(tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(tree.get("emptyfolder")));
        Files.delete(zip);
    }

    @Test
    public void testImportFile() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree tree = JkPathTree.of(dirSample);
        Path tempFile = Files.createTempFile("example", ".txt");
        tree.copyIn(tempFile);
        assertTrue(Files.exists(tree.get(tempFile.getFileName().toString())));
        Files.delete(tempFile);
    }

    @Test
    public void testZipGet() throws Exception {
        Path zipFile = createSampleZip();
        JkPathTree zipTree = JkPathTree.ofZip(zipFile);
        Path zipEntry = zipTree.get("/subfolder/sample.txt");
        assertTrue(Files.exists(zipEntry));
        assertFalse(Files.exists(zipTree.get("/opopkhjkjkjh")));
        Files.delete(zipFile);
    }

    @Test
    public void testDeleteContent() throws IOException {
        Path foo = Files.createTempDirectory("foo");
        Path bar = foo.resolve("bar");
        Files.createDirectories(bar);
        Path txt = bar.resolve("file.txt");
        Files.write(txt, "toto".getBytes(Charset.forName("UTF8")));
        Path txt2 = foo.resolve("file2.txt");
        Files.copy(txt, txt2);
        assertTrue(Files.exists(txt));
        JkPathTree fooTree = JkPathTree.of(foo).andReject("bar/**", "bar");
        System.out.println(fooTree.getFiles());
        assertFalse(fooTree.getFiles().contains(txt));
        fooTree.deleteContent();
        assertTrue(Files.exists(txt));
        fooTree.deleteRoot();  // cleanup
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

    private static Path createSampleZip() throws Exception {
        Path folder = sampleDir();
        Path zip = Files.createTempFile("sample", ".zip");
        Files.delete(zip);
        JkPathTree.of(folder).zipTo(zip);
        return zip;
    }

}
