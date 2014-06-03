package org.jake.file.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.List;

import org.jake.file.utils.FileUtils;
import org.junit.Test;

public class FileUtilsTest {

	@Test
	public void testFileOf() throws Exception {
		URL sampleFileUrl = FileUtilsTest.class.getResource("samplefolder/subfolder/sample.txt");
		File sampleFile = new File(sampleFileUrl.toURI().getPath());
		assertTrue(sampleFile.exists());
		File sampleDir = sampleFile.getParentFile().getParentFile();
		
		List<File> files = FileUtils.filesOf(sampleDir, false);
		assertEquals(1, files.size());
		
		files = FileUtils.filesOf(sampleDir, true);
		assertEquals(2, files.size());
		
		FileFilter fileFilter = new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return !pathname.getName().equals("sample.txt");
			}
		};
		
		files = FileUtils.filesOf(sampleDir, fileFilter, false);
		assertEquals(0, files.size());
		
		files = FileUtils.filesOf(sampleDir, fileFilter, true);
		assertEquals(1, files.size());
	}
	
	

}
