package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.publication.JkMavenMetadata;
import org.junit.jupiter.api.Test;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("javadoc")
class JkMavenMetadataTest {

    @Test
   void testupdateSnapshot() throws UnsupportedEncodingException {
        final JkMavenMetadata mavenMetadata = JkMavenMetadata.of(JkCoordinate.of("dev.jeka:core:0.1-SNAPSHOT"), "11111111.222222");
        mavenMetadata.updateSnapshot("20151023145532");
        mavenMetadata.addSnapshotVersion("jar", "source");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mavenMetadata.output(outputStream);
        final String string = outputStream.toString("UTF-8");
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
        final JkMavenMetadata readData = JkMavenMetadata.of(inputStream);
        outputStream = new ByteArrayOutputStream();
        readData.output(outputStream);
        final String string2 = outputStream.toString("UTF-8");
        System.out.println(string2);
        assertEquals(string, string2);

    }

    @Test
    void testAddRelease() throws UnsupportedEncodingException {
        final JkMavenMetadata mavenMetadata = JkMavenMetadata.of(JkModuleId.of("dev.jeka", "core"));
        mavenMetadata.addVersion("1.3.2", "20151023145532");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mavenMetadata.output(outputStream);
        final String string = outputStream.toString("UTF-8");
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
        final JkMavenMetadata readData = JkMavenMetadata.of(inputStream);
        outputStream = new ByteArrayOutputStream();
        readData.output(outputStream);
        final String string2 = outputStream.toString("UTF-8");
        System.out.println(string2);
        assertEquals(string, string2);
    }

    @Test
    void testAddSnapshot() throws UnsupportedEncodingException {
        final JkMavenMetadata mavenMetadata = JkMavenMetadata.of(JkModuleId.of("dev.jeka", "core"));
        mavenMetadata.addVersion("1.3.2-SNAPSHOT", "20151023145532");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mavenMetadata.output(outputStream);
        final String string = outputStream.toString("UTF-8");
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
        final JkMavenMetadata readData = JkMavenMetadata.of(inputStream);
        outputStream = new ByteArrayOutputStream();
        readData.output(outputStream);
        final String string2 = outputStream.toString("UTF-8");
        System.out.println(string2);
        assertEquals(string, string2);
    }

}
