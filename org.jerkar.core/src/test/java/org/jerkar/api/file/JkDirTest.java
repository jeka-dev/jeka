package org.jerkar.api.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jerkar.api.file.JkFileTree;
import org.junit.Test;

public class JkDirTest {

	@Test
	public void testFileList() throws Exception {
		final URL sampleFileUrl = JkDirTest.class.getResource("../utils/samplefolder/subfolder/sample.txt");
		final File sampleFile = new File(sampleFileUrl.toURI().getPath());
		assertTrue(sampleFile.exists());
		final File sampleFolder = sampleFile.getParentFile().getParentFile();

		JkFileTree subfolderTxt = JkFileTree.of(sampleFolder).include("/subfolder/*.txt");
		assertEquals(1, subfolderTxt.files(false).size());

		subfolderTxt = JkFileTree.of(sampleFolder).include("subfolder/*.txt");
		assertEquals(1, subfolderTxt.files(false).size());

	}

}
