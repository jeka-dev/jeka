package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsPathTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class JkPathTreeTest {

    @Test
    void testFilesOnly() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final Path sampleFile = Paths.get(sampleFileUrl.toURI());
        Assertions.assertTrue(Files.exists(sampleFile));
        final Path sampleFolder = sampleFile.getParent().getParent();

        System.out.println(JkPathTree.of(sampleFolder).stream().relativizeFromRoot().collect(Collectors.toList()));

        final JkPathTree subfolderTxt1 = JkPathTree.of(sampleFolder).andMatching(true, "subfolder/*.txt");
        Assertions.assertEquals(1, subfolderTxt1.getFiles().size());
        System.out.println(subfolderTxt1);

        final JkPathTree subfolderTxt2 = JkPathTree.of(sampleFolder).andMatching(true, "subfolder/*.txt");
        Assertions.assertEquals(1, subfolderTxt2.getFiles().size());
    }

    @Test
    void testRelativeFiles() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final JkPathTree tree = JkPathTree.of(Paths.get(sampleFileUrl.toURI()).getParent().getParent());
        System.out.println(tree.stream().relativizeFromRoot().collect(Collectors.toList()));
    }

    @Test
    void testStream() throws Exception {
        Path sampleDir = sampleDir();

        // Root is included in the getOutputStream
        Assertions.assertTrue(JkPathTree.of(sampleDir).stream().anyMatch(path -> path.equals(sampleDir)));
    }

    @Test
    void testZipTo() throws Exception {
        Path zip = Files.createTempFile("filetree", ".zip");
        Files.delete(zip);
        JkPathTree.of(sampleDir()).zipTo(zip);
        try (JkUtilsPath.JkZipRoot zipRoot = JkUtilsPath.zipRoot(zip)) {
            Assertions.assertTrue(Files.exists(zipRoot.get().resolve("subfolder/sample.txt")));
        }

        // Test with match
        Files.delete(zip);
        JkPathTree.of(sampleDir()).andMatching(false, "**r/sample.txt").zipTo(zip);
        try (JkUtilsPath.JkZipRoot zipRoot = JkUtilsPath.zipRoot(zip)) {
            Assertions.assertFalse(Files.exists(zipRoot.get().resolve("subfolder/sample.txt")));
        }
    }

    @Test
    void testOfZip() throws Exception {
        Path zipFile = Files.createTempFile("jksample",".zip");
        Files.deleteIfExists(zipFile);
        JkZipTree zipTree = JkZipTree.of(zipFile);
        zipTree.importDir(sampleDir());
        List<Path> paths = zipTree.getFiles();
        Assertions.assertEquals(1, paths.size());

        //System.out.println(Files.exists(zipRoot));
        //zipRoot.getFileSystem().close();
        Files.delete(zipFile);
    }

    @Test  // Ensure we can create a zip from a zip
    void testZipZipTo() throws Exception {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkZipTree.of(zip).zipTo(zip2);
        JkZipTree zip2Tree = JkZipTree.of(zip2);
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test  // Ensure we can import  a zip from a zip
    void testZipImportTree() throws Exception {
        Path zip = createSampleZip();
        Path zip2 = Files.createTempFile("sample2", ".zip");
        Files.delete(zip2);
        JkZipTree zip2Tree = JkZipTree.of(zip2);
        zip2Tree.importTree(JkZipTree.of(zip));
        assertTrue(Files.isDirectory(zip2Tree.get("subfolder")));
        assertTrue(Files.isRegularFile(zip2Tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(zip2Tree.get("emptyfolder")));
        Files.delete(zip);
        Files.delete(zip2);
    }

    @Test
    void testImportTree() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree tree = JkPathTree.of(dirSample);
        tree.importTree(JkZipTree.of(createSampleZip()));
        assertTrue(Files.isDirectory(tree.get("subfolder")));
        assertTrue(Files.isRegularFile(tree.get("subfolder").resolve("sample.txt")));
        assertTrue(Files.isDirectory(tree.get("emptyfolder")));
    }

    @Test
    void testImportFiles() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree tree = JkPathTree.of(dirSample);
        Path tempFile = Files.createTempFile("example", ".txt");
        tree.importFiles(tempFile);
        assertTrue(Files.exists(tree.get(tempFile.getFileName().toString())));
        Files.delete(tempFile);
    }

    @Test
    void testImportFile() throws Exception {
        Path dirSample = Files.createTempDirectory("sample");
        JkPathTree treeSample = JkPathTree.of(dirSample);
        testImportFile(treeSample);
        Path zipFile = createSampleZip();
        try (JkZipTree zipTree = JkZipTree.of(zipFile)) {
            testImportFile(zipTree);
        }
    }

    private void testImportFile(JkAbstractPathTree<?> treeSample) throws URISyntaxException {
        Path sampleTxt = Paths.get(JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt").toURI());

        treeSample.importFile(sampleTxt, "newDir/sample.txt");
        treeSample.importFile(sampleTxt, "sample.txt");

        assertTrue(Files.exists(treeSample.getRoot().resolve("sample.txt")));
        assertTrue(Files.exists(treeSample.getRoot().resolve("newDir/sample.txt")));

    }

    @Test
    void testZipGet() throws Exception {
        Path zipFile = createSampleZip();
        JkZipTree zipTree = JkZipTree.of(zipFile);
        Path zipEntry = zipTree.get("/subfolder/sample.txt");
        assertTrue(Files.exists(zipEntry));
        assertFalse(Files.exists(zipTree.get("/opopkhjkjkjh")));
        Files.delete(zipFile);
    }

    @Test
    void testDeleteContent() throws IOException {
        Path foo = Files.createTempDirectory("foo");
        Path bar = foo.resolve("bar");
        Files.createDirectories(bar);
        Path txt = bar.resolve("file.txt");
        Files.write(txt, "toto".getBytes(StandardCharsets.UTF_8));
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
    void testZipDeleteContent() throws Exception {
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
    void testAndMatching() throws Exception {
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
    void stream_relativeToCurrentDir_orAbsolute() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path sampleFolder = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Path first = JkPathTree.of(sampleFolder).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No sample file found"));

        // test with absolute root folder
        assertTrue(sampleFolder.isAbsolute());
        assertTrue(first.isAbsolute());

        // Test with root being a relative pah
        Path root = Paths.get("").toAbsolutePath().relativize(sampleFolder).normalize();
        assertTrue(Files.exists(root), "Root does not exist");
        assertFalse(root.isAbsolute());
        JkPathTree relPathTree = JkPathTree.of(root);
        first = relPathTree.stream()
                .filter(path -> !Files.isDirectory(path))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No sample file found"));
        assertTrue(Files.exists(first));
        System.out.println(root);
        System.out.println(first);

    }

    @Test
    void streamBreathFirst_orderIsOk() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path sampleFolder = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        JkPathTree pathTree = JkPathTree.of(sampleFolder);
        pathTree.streamBreathFirst().forEach(System.out::println);
        List<Path> paths = pathTree.streamBreathFirst().collect(Collectors.toList());
        assertTrue(
                paths.get(3).startsWith(paths.get(1))
                || paths.get(3).startsWith(paths.get(2))
        );
    }

    @Test
    void stream_relativizeFromRootIsOk() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path sampleFolder = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        JkPathTree pathTree = JkPathTree.of(sampleFolder);
        pathTree.stream().filter(Files::isDirectory).forEach(System.out::println);
        pathTree.stream().relativizeFromRoot().forEach(System.out::println);
    }

    @Test
    @Disabled  // This test fails when launched in forked mode
    void testAndMatching_rootIsWorkingDir() throws Exception {
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
    void testCopyToWithFilter() throws Exception {
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
    void testCopyTo() throws Exception {
        Path zipFile = createSampleZip();
        Path tempDir = Files.createTempDirectory("jeka-test");
        try (JkZipTree tree = JkZipTree.of(zipFile)) {
            tree.goTo("subfolder").andMatching("sample.txt").copyTo(tempDir);
        }
        assertTrue(Files.exists(tempDir.resolve("sample.txt")));
    }

    @Test
    void testCopyToSingle() throws Exception {
        Path zipFile = createSampleZip();
        Path tempDir = Files.createTempDirectory("jeka-test");
        try (JkZipTree pathTree = JkZipTree.of(zipFile)) {
            pathTree.copyFile("subfolder/sample.txt", tempDir);
        }
        assertTrue(Files.exists(tempDir.resolve("sample.txt")));
    }

    @Test
    void testZipStreamWithNoDirectoryMatcher() throws Exception {
        JkZipTree zipTree = JkZipTree.of(createSampleZip());
        try (Stream<Path> stream = zipTree.withMatcher(JkPathMatcher.ofNoDirectory()).stream()) {
            stream.forEach(System.out::println);
        }
    }

}
