package org.jerkar.api.depmanagement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

public class MavenMetadataTest {

    @Test
    public void testupdateSnapshot() throws UnsupportedEncodingException {
	final MavenMetadata mavenMetadata = MavenMetadata
		.of(JkModuleId.of("org.jerkar", "core").version("0.1-SNAPSHOT"));
	mavenMetadata.updateSnapshot();
	mavenMetadata.addSnapshotVersion("jar", "source");
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	mavenMetadata.output(outputStream);
	final String string = outputStream.toString("UTF-8");
	final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
	final MavenMetadata readData = MavenMetadata.of(inputStream);
	outputStream = new ByteArrayOutputStream();
	readData.output(outputStream);
	final String string2 = outputStream.toString("UTF-8");
	System.out.println(string2);
	Assert.assertEquals(string, string2);

    }

    @Test
    public void testAddRelease() throws UnsupportedEncodingException {
	final MavenMetadata mavenMetadata = MavenMetadata.of(JkModuleId.of("org.jerkar", "core"));
	mavenMetadata.addVersion("1.3.2");
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	mavenMetadata.output(outputStream);
	final String string = outputStream.toString("UTF-8");
	final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
	final MavenMetadata readData = MavenMetadata.of(inputStream);
	outputStream = new ByteArrayOutputStream();
	readData.output(outputStream);
	final String string2 = outputStream.toString("UTF-8");
	System.out.println(string2);
	Assert.assertEquals(string, string2);
    }

    @Test
    public void testAddSnapshot() throws UnsupportedEncodingException {
	final MavenMetadata mavenMetadata = MavenMetadata.of(JkModuleId.of("org.jerkar", "core"));
	mavenMetadata.addVersion("1.3.2-SNAPSHOT");
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	mavenMetadata.output(outputStream);
	final String string = outputStream.toString("UTF-8");
	final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
	final MavenMetadata readData = MavenMetadata.of(inputStream);
	outputStream = new ByteArrayOutputStream();
	readData.output(outputStream);
	final String string2 = outputStream.toString("UTF-8");
	System.out.println(string2);
	Assert.assertEquals(string, string2);
    }

}
