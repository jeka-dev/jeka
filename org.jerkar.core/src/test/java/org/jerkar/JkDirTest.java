package org.jerkar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jerkar.JkDir;
import org.junit.Test;

public class JkDirTest {

	@Test
	public void testFileList() throws Exception {
		URL sampleFileUrl = JkDirTest.class.getResource("utils/samplefolder/subfolder/sample.txt");
		File sampleFile = new File(sampleFileUrl.toURI().getPath());
		assertTrue(sampleFile.exists());
		File sampleFolder = sampleFile.getParentFile().getParentFile();
		
		JkDir subfolderTxt = JkDir.of(sampleFolder).include("/subfolder/*.txt");
		assertEquals(1, subfolderTxt.files().size());
		
		subfolderTxt = JkDir.of(sampleFolder).include("subfolder/*.txt");
		assertEquals(1, subfolderTxt.files().size());
		
	}

}
