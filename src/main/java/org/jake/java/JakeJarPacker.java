package org.jake.java;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDirSet;
import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.JakeZip;
import org.jake.java.build.JakeBuildJava;
import org.jake.utils.JakeUtilsFile;

public class JakeJarPacker {

	private final JakeBuildJava build;

	private final boolean fatJar;

	private final int compressionLevel;

	private final boolean includeVersion;

	private final boolean fullName;

	private final boolean checkSum;

	protected JakeJarPacker(JakeBuildJava buildBase) {
		this(buildBase, false, false, Deflater.DEFAULT_COMPRESSION, false, true);
	}

	private JakeJarPacker(JakeBuildJava buildBase, boolean includeVersion, boolean fatJar, int compressionLevel, boolean fullName, boolean checkSum) {
		super();
		this.build = buildBase;
		this.fatJar = fatJar;
		this.compressionLevel = compressionLevel;
		this.includeVersion = includeVersion;
		this.fullName = fullName;
		this.checkSum = checkSum;
	}

	public static JakeJarPacker of(JakeBuildJava buildJava) {
		return new JakeJarPacker(buildJava);
	}

	public JakeJarPacker withFatFar(boolean fatJar) {
		return new JakeJarPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	public JakeJarPacker withCompression(int compressionLevel) {
		return new JakeJarPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	public JakeJarPacker withIncludeVersion(boolean includeVersion) {
		return new JakeJarPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	public JakeJarPacker withFullName(boolean fullName) {
		return new JakeJarPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	protected JakeBuildJava build() {
		return build;
	}

	public String baseName() {
		final String name = fullName ? build.projectFullName() : build.projectName();
		if (includeVersion) {
			return name + "-" + build.version();
		}
		return name;
	}

	protected int zipLevel() {
		return Deflater.DEFAULT_COMPRESSION;
	}

	public File jarFile() {
		return build().ouputDir(baseName() + ".jar");
	}

	public File jarSourceFile() {
		return build().ouputDir(baseName() + "-sources.jar");
	}

	public File jarTestFile() {
		return build().ouputDir(baseName() + "-test.jar");
	}

	public File jarTestSourceFile() {
		return build().ouputDir(baseName() + "-test-sources.jar");
	}

	public File fatJarFile() {
		return build().ouputDir(baseName() + "-fat.jar");
	}

	public void pack() {
		JakeLog.startAndNextLine("Packaging module");
		makeZip(JakeDirSet.of(build().classDir()), jarFile());
		makeZip(build().sourceDirs().and(build().resourceDirs()), jarSourceFile());
		if (!build().skipTests()) {
			makeZip(JakeDirSet.of(build().testClassDir()), jarTestFile());
		}
		makeZip(build().testSourceDirs().and(build().testResourceDirs()), jarTestSourceFile());
		if (fatJar) {
			JakeZip.of(build().classDir()).merge(build().deps().runtimeScope()).create(fatJarFile(), compressionLevel);
		}
		if (checkSum) {
			checksum();
		}
		JakeLog.done();
	}

	private void makeZip(JakeDirSet dirSet, File dest) {
		JakeLog.start("Creating file : " + dest.getPath());
		dirSet.zip(dest, zipLevel());
		JakeLog.done();
	}

	@JakeDoc("Create MD5 check sum for both regular and fat jar files.")
	private void checksum() {
		final File file = build().ouputDir(baseName() + ".md5");
		JakeLog.info("Creating file : " + file);
		JakeUtilsFile.writeString(file, JakeUtilsFile.md5Checksum(jarFile()), false);
		if (fatJarFile().exists()) {
			final File fatSum = build().ouputDir(baseName() + "-fat" + ".md5");
			JakeLog.info("Creating file : " + fatSum);
			JakeUtilsFile.writeString(fatSum, JakeUtilsFile.md5Checksum(fatJarFile()), false);
		}
	}


}
