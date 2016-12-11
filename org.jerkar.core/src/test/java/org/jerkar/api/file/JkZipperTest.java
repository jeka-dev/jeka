package org.jerkar.api.file;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;



@SuppressWarnings("javadoc")
public class JkZipperTest {

    // Test that generated zip is properly closed
    @Test
    public void testToAndDelete() throws Exception {
        final URL url = this.getClass().getResource(JkZipperTest.class.getSimpleName() + ".class");
        final File dir = new File(url.getFile()).getParentFile();
        final File tempZip = File.createTempFile("oooo", ".zip");
        System.out.println(dir);
        JkFileTree.of(dir).from("..").zip().to(tempZip);
        Assert.assertTrue("Can't delete " + tempZip, tempZip.delete());
    }

}
