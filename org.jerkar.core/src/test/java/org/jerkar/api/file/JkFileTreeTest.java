package org.jerkar.api.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

}
