package org.jerkar.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;


@SuppressWarnings("javadoc")
public class JkUtilsFileTest {

    @Test
    public void testFileOf() throws Exception {
        final URL sampleFileUrl = JkUtilsFileTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        final File sampleFile = new File(sampleFileUrl.toURI().getPath());
        assertTrue(sampleFile.exists());
        final File sampleDir = sampleFile.getParentFile().getParentFile();

        List<File> files = JkUtilsFile.filesOf(sampleDir, false);
        assertEquals(1, files.size());

        files = JkUtilsFile.filesOf(sampleDir, true);
        assertEquals(3, files.size());

        final FileFilter fileFilter = pathname -> !pathname.getName().equals("sample.txt");

        files = JkUtilsFile.filesOf(sampleDir, fileFilter, false);
        assertEquals(0, files.size());

        files = JkUtilsFile.filesOf(sampleDir, fileFilter, true);
        assertEquals(2, files.size());


    }

    @Test
    @Ignore // JVM bug test
    public void testNoNullPointerEx() {
        final FileFilter fileFilter = pathname -> !pathname.getName().equals("sample.txt");
        JkUtilsFile.filesOf(new File("c:/users"), fileFilter, true);
    }

    @Test
    public void testRelativePath() throws IOException {
        final File base = new File("C:/my/asScopedDependency/folder");
        final File file1 = new File("C:/my/asScopedDependency/folder/foo/bar.txt");
        final File file2 = new File("C:/my/asScopedDependency/foo/bar.txt");
        assertEquals("foo" + File.separator + "bar.txt", JkUtilsFile.getRelativePath(base, file1));
        assertEquals(".." + File.separator + "foo" + File.separator + "bar.txt", JkUtilsFile.getRelativePath(base, file2));
    }

}
