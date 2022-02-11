package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsPathTest;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

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

        final JkPathTree subfolderTxt1 = JkPathTree.of(sampleFolder).andMatching(true, "subfolder/*.txt");
        assertEquals(1, subfolderTxt1.getFiles().size());
        System.out.println(subfolderTxt1);

        final JkPathTree subfolderTxt2 = JkPathTree.of(sampleFolder).andMatching(true, "subfolder/*.txt");
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
        try (JkUtilsPath.JkZipRoot zipRoot = JkUtilsPath.zipRoot(zip)) {
            assertTrue(Files.exists(zipRoot.get().resolve("subfolder/sample.txt")));
        }

        // Test with match
        Files.delete(zip);
        JkPathTree.of(sampleDir()).andMatching(false, "**r/sample.txt").zipTo(zip);
        try (JkUtilsPath.JkZipRoot zipRoot = JkUtilsPath.zipRoot(zip)) {
            assertFalse(Files.exists(zipRoot.get().resolve("subfolder/sample.txt")));
        }
    }

    @Test
    public void testOfZip() throws Exception {
        Path zipFile = Files.createTempFile("jksample",".zip");
        Files.deleteIfExists(zipFile);
        JkZipTree zipTree = JkZipTree.of(zipFile);
        zipTree.importDir(sampleDir());
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
        JkZipTree.of(zip).zipTo(zip2);
        JkPathTree zip2Tree = JkZipTree.of(zip2);
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test  // Ensure we can import  a zip from a zip
    public void testZipImportTree() throws Exception {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkPathTree zip2Tree = JkZipTree.of(zip2);
        zip2Tree.importTree(JkZipTree.of(zip));
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test
    public void testImportTree() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree tree = JkPathTree.of(dirSample);
        tree.importTree(JkZipTree.of(createSampleZip()));
        assertTrue(Files.isDirectory(tree.get("subfolder")));
        assertTrue(Files.isRegularFile(tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(tree.get("emptyfolder")));
    }

    @Test
    public void testImportFiles() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree tree = JkPathTree.of(dirSample);
        Path tempFile = Files.createTempFile("example", ".txt");
        tree.importFiles(tempFile);
        assertTrue(Files.exists(tree.get(tempFile.getFileName().toString())));
        Files.delete(tempFile);
    }

    @Test
    public void testImportFile() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree treeSample = JkPathTree.of(dirSample);
        testImportFile(treeSample);
        Path zipFile = createSampleZip();
        try (JkZipTree zipTree = JkZipTree.of(zipFile)) {
            testImportFile(zipTree);
        }
    }

    private void testImportFile(JkPathTree treeSample) throws URISyntaxException {
        Path sampleTxt = Paths.get(JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt").toURI());

        treeSample.importFile(sampleTxt, "newDir/sample.txt");
        treeSample.importFile(sampleTxt, "sample.txt");

        assertTrue(Files.exists(treeSample.getRoot().resolve("sample.txt")));
        assertTrue(Files.exists(treeSample.getRoot().resolve("newDir/sample.txt")));

    }

    @Test
    public void testZipGet() throws Exception {
        Path zipFile = createSampleZip();
        JkPathTree zipTree = JkZipTree.of(zipFile);
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
        JkPathTree fooTree = JkPathTree.of(foo).andMatching(false, "bar/**", "bar");
        System.out.println(fooTree.getFiles());
        assertFalse(fooTree.getFiles().contains(txt));
        fooTree.deleteContent();
        assertTrue(Files.exists(txt));
        fooTree.deleteRoot();  // cleanup
    }

    @Test
    public void testZipDeleteContent() throws Exception {
        Path zip = createSampleZip();
        boolean subfolderExist = Files.exists(JkZipTree.of(zip).goTo("subfolder").getRoot());
        assertTrue(subfolderExist);
        JkZipTree.of(zip).goTo("subfolder").deleteRoot().close();
        subfolderExist = Files.exists(JkZipTree.of(zip).goTo("subfolder").getRoot());
        assertFalse(subfolderExist);
        Files.delete(zip);
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

    @Test
    public void testAndMatching() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path sampleFolder = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Stream<Path> stream = JkPathTree.of(sampleFolder).andMatching(false, "subfolder/**").stream();
        boolean samplePresent = stream
                .anyMatch(path -> path.getFileName().toString().equals("sample.txt"));
        assertFalse(samplePresent);
        JkPathTree tree = JkPathTree.of(sampleFolder);
        assertFalse(tree.andMatching("subfolder/*.txt").getFiles().isEmpty());
        assertFalse(tree.andMatching("subfolder/*.txt", "*.txt").getFiles().isEmpty());
        assertFalse(tree.andMatching("subfolder/*.txt", "*.java").getFiles().isEmpty());
    }

    @Test
    @Ignore  // This test fails when launched in forked mode
    public void testAndMatching_rootIsWorkingDir() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        Path sampleFolder = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        sampleFolder = JkUtilsPath.relativizeFromWorkingDir(sampleFolder);
        JkPathTree WorkingDirTree = JkPathTree.of("");
        String filter = sampleFolder.toString().replace('\\', '/') + "/subfolder/*.txt";
        List<Path> files = WorkingDirTree.andMatching(filter).getFiles();
        assertFalse(files.isEmpty());
    }

    @Test
    public void testCopyToWithFilter() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path sampleFolder = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Path tempDir = Files.createTempDirectory("jeka-test");
        JkPathTree tree = JkPathTree.of(sampleFolder).andMatching(false, "subfolder/**");
        JkPathMatcher matcher = tree.getMatcher();
        assertFalse(matcher.matches(Paths.get("subfolder/sample.txt")));
        tree.copyTo(tempDir);
        assertFalse(Files.exists(tempDir.resolve("subfolder/sample.txt")));
    }

    @Test
    public void testCopyTo() throws Exception {
        Path zipFile = createSampleZip();
        Path tempDir = Files.createTempDirectory("jeka-test");
        try (JkZipTree tree = JkZipTree.of(zipFile)) {
            tree.goTo("subfolder").andMatching("sample.txt").copyTo(tempDir);
        }
        assertTrue(Files.exists(tempDir.resolve("sample.txt")));
    }

    @Test
    public void testCopyToSingle() throws Exception {
        Path zipFile = createSampleZip();
        Path tempDir = Files.createTempDirectory("jeka-test");
        try (JkZipTree pathTree = JkZipTree.of(zipFile)) {
            pathTree.copyFile("subfolder/sample.txt", tempDir);
        }
        assertTrue(Files.exists(tempDir.resolve("sample.txt")));
    }

    @Test
    public void testZipStreamWithNoDirectoryMatcher() throws Exception {
        JkZipTree zipTree = JkZipTree.of(createSampleZip());
        try (Stream<Path> stream = zipTree.withMatcher(JkPathMatcher.ofNoDirectory()).stream()) {
            stream.forEach(System.out::println);
        }
    }

}
