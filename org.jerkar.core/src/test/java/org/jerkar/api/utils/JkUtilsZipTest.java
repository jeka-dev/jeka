package org.jerkar.api.utils;

import java.io.File;
import java.io.InputStream;

import org.jerkar.api.file.JkZipFile;
import org.junit.Assert;
import org.junit.Test;

public class JkUtilsZipTest {

    @Test
    public void testReadZipEntry() throws Exception {
        final File file = JkUtilsFile.resourceAsFile(JkUtilsZipTest.class, "toto.zip");
        final JkZipFile zipFile = JkZipFile.of(file);
        final InputStream toto1is = zipFile.inputStream("toto1.zip");
        final InputStream toto2is = JkUtilsZip.readZipEntry(toto1is, "toto2.txt");
        final String content = JkUtilsIO.readAsString(toto2is);
        Assert.assertTrue(content.startsWith("toto2content"));
        JkUtilsIO.closeQuietly(zipFile, toto1is, toto2is);
    }

}
