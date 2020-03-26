package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkPathSequenceTest {

    @Test
    public void testSerialize() {
        Path path1 = Paths.get("path1");
        Path path2 = Paths.get("path2");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JkUtilsIO.serialize(JkPathSequence.of(path1, path2), baos);
        JkPathSequence deser = JkUtilsIO.deserialize(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(path1, deser.getEntries().get(0));
        Assert.assertEquals(path2, deser.getEntries().get(1));
        Assert.assertEquals(2, deser.getEntries().size());
    }

}