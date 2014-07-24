package org.jake;

import org.jake.file.utils.JakeUtilsFile;

public class JakeLauncher {

	public static void main(String[] args) {
		final ProjectBuilder projectBuilder = new ProjectBuilder(JakeUtilsFile.workingDir());
		final boolean result = projectBuilder.build();
		if (!result) {
			System.exit(1);
		}
	}

}
