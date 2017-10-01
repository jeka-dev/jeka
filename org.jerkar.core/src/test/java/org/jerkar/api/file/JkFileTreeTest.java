package org.jerkar.api.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.utils.JkUtilsPathTest;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkFileTreeTest {

    @Test
    public void testFileList() throws Exception {
        final URL sampleFileUrl = JkFileTreeTest.class
                .getResource("../utils/samplefolder/subfolder/sample.txt");
        final File sampleFile = new File(sampleFileUrl.toURI().getPath());
        assertTrue(sampleFile.exists());
        final File sampleFolder = sampleFile.getParentFile().getParentFile();

        JkFileTree subfolderTxt = JkFileTree.of(sampleFolder).include("/subfolder/*.txt");
        assertEquals(1, subfolderTxt.files(false).size());

        subfolderTxt = JkFileTree.of(sampleFolder).include("subfolder/*.txt");
        assertEquals(1, subfolderTxt.files(false).size());
    }

    @Test
    public void testRelativeFiles() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Files.createDirectories(source.resolve("emptyfolder"));   // git won't copy empty dir

        final JkFileTree tree = JkFileTree.of(Paths.get(sampleFileUrl.toURI()).getParent().getParent());
        System.out.println(tree.allRelativePaths());
    }

}
