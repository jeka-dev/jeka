
import java.io.File;

import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;
import org.jake.utils.JakeUtilsTime;

public class Build extends JakeBuildJar {

	public static void main(String[] args) {
		new Build().doDefault();
	}

	@Override
	protected String projectName() {
		return "jake";
	}

	@Override
	public void copyResources() {
		super.copyResources();
		JakeUtilsFile.writeString(new File(classDir(),"org/jake/version.txt"), JakeUtilsTime.timestampSec(), false);
	}

	@Override
	public void doDefault() {
		help();
		super.doDefault();
		distrib();
	}

	@JakeDoc("Creates a zip file containing the binaries for Jake full install.")
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
