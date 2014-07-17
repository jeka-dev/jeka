

import java.io.File;

import org.jake.file.utils.FileUtils;
import org.jake.java.JakeJarBuild;

public class Build extends JakeJarBuild {

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
		FileUtils.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final File jarFile = buildOuputDir(jarName()+".jar");
		FileUtils.copyFileToDir(jarFile, distribDir);
		FileUtils.copyFileToDir(buildOuputDir(jarName() + "-sources.jar"), distribDir);
		FileUtils.zipDir(buildOuputDir("jake-distrib.zip"), zipLevel(), distribDir);
	}

}
