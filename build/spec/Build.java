
import java.io.File;

import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.file.JakeDirSet;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;
import org.jake.java.JakeJavaCompiler;
import org.jake.utils.JakeUtilsTime;

public class Build extends JakeBuildJar {

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new Build().base();
		//new Build().javadoc();
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
	protected JakeJavaCompiler compiler(JakeDirSet sources, File outputDir,
			Iterable<File> classpath) {
		return super.compiler(sources, outputDir, classpath); //.addOption("-Xlint:unchecked");
	}

	@Override
	protected void afterPackJars() {
		final File distribDir = ouputDir("jake-distrib");
		final File distripZipFile = ouputDir("jake-distrib.zip");
		JakeLog.start("Creating distrib " + distripZipFile.getPath());
		JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final File jarFile = ouputDir(jarName()+".jar");
		JakeUtilsFile.copyFileToDir(jarFile, distribDir);
		JakeUtilsFile.copyFileToDir(ouputDir(jarName() + "-sources.jar"), distribDir);
		JakeUtilsFile.zipDir(distripZipFile, zipLevel(), distribDir);
		JakeLog.done();
	}

}
