
import java.io.File;

import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;
import org.jake.utils.JakeUtilsTime;

public class Build extends JakeBuildJar {

	public static void main(String[] args) {
		new Build().base();
	}

	@Override
	protected String projectName() {
		return "jake";
	}

	@Override
	protected void processResources() {
		super.processResources();
		JakeUtilsFile.writeString(new File(classDir(),"org/jake/version.txt"), JakeUtilsTime.timestampSec(), false);
	}

	@Override
	@JakeDoc("Compile, unit test and package Jake application in a distribution file.")
	public void base() {
		super.base();
		distrib();
	}

	private void distrib() {
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
