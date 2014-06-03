package org.jake.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jake.file.DirView;
import org.junit.Test;

public class DirViewTest {

	@Test
	public void testFileList() throws Exception {
		URL sampleFileUrl = DirViewTest.class.getResource("utils/samplefolder/subfolder/sample.txt");
		File sampleFile = new File(sampleFileUrl.toURI().getPath());
		assertTrue(sampleFile.exists());
		File sampleFolder = sampleFile.getParentFile().getParentFile();
		
		DirView subfolderTxt = DirView.of(sampleFolder).include("/subfolder/*.txt");
		assertEquals(1, subfolderTxt.listFiles().size());
		
		subfolderTxt = DirView.of(sampleFolder).include("subfolder/*.txt");
		assertEquals(1, subfolderTxt.listFiles().size());
		
	}

}
