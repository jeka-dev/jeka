

import java.io.File;

import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;

public class Build extends JakeBuildJar {

	@Override
	protected String projectName() {
		return "jake";
	}

	public static void main(String[] args) {
		new Build().doDefault();
	}

	@Override
	public void doDefault() {
		help();
		super.doDefault();
		distrib();

	}

	public void distrib() {
		JakeLog.start("Packaging distrib");
		final File distribDir = buildOuputDir("jake-distrib");
		JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final File jarFile = buildOuputDir(jarName()+".jar");
		JakeUtilsFile.copyFileToDir(jarFile, distribDir);
		JakeUtilsFile.copyFileToDir(buildOuputDir(jarName() + "-sources.jar"), distribDir);
		final File distripZipFile = buildOuputDir("jake-distrib.zip");
		JakeUtilsFile.zipDir(distripZipFile, zipLevel(), distribDir);
		JakeLog.done(distripZipFile.getPath() + " created");
	}



}
