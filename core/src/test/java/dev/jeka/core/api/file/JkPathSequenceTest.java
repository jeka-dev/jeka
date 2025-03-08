package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

class JkPathSequenceTest {

    @Test
    void testSerialize() {
        Path path1 = Paths.get("path1");
        Path path2 = Paths.get("path2");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JkUtilsIO.serialize(JkPathSequence.of(path1, path2), baos);
        JkPathSequence deser = JkUtilsIO.deserialize(new ByteArrayInputStream(baos.toByteArray()));
        Assertions.assertEquals(path1, deser.getEntries().get(0));
        Assertions.assertEquals(path2, deser.getEntries().get(1));
        Assertions.assertEquals(2, deser.getEntries().size());
    }

}