

import java.io.File;

import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;

public class Build extends JakeBuildJar {

	@Override
	protected String projectName() {
		return "jake";
	}

	@Override
	protected String version() {
		return "0.1-SNAPSHOT";
	}

	@Override
	protected String jarName() {
		return this.projectName();  // Don't need the version info within the name
	}

	public static void main(String[] args) {
		final Build build = new Build();
		build.doDefault();
		//build.javadoc();
		build.distrib();
	}

	public void distrib() {
		final File distribDir = buildOuputDir("jake-distrib");
		JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final File jarFile = buildOuputDir(jarName()+".jar");
		JakeUtilsFile.copyFileToDir(jarFile, distribDir);
		JakeUtilsFile.copyFileToDir(buildOuputDir(jarName() + "-sources.jar"), distribDir);
		JakeUtilsFile.zipDir(buildOuputDir("jake-distrib.zip"), zipLevel(), distribDir);
	}

}
