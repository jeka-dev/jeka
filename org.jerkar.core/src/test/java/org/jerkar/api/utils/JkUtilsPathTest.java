package org.jerkar.api.utils;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@SuppressWarnings("javadoc")
public class JkUtilsPathTest {

    @Test
    public void testCopyDir() throws Exception {
        final URL sampleFileUrl = JkUtilsPathTest.class
                .getResource("samplefolder/subfolder/sample.txt");
        Path source = Paths.get(sampleFileUrl.toURI()).getParent().getParent();
        Path target = Files.createTempDirectory("copydirtest");
        JkUtilsPath.copyDirContent(source, target);
        System.out.println(target);
        assertTrue(Files.exists(target.resolve("subfolder/sample.txt")));


    }


}
