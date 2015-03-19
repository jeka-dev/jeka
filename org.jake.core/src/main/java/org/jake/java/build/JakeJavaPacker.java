package org.jake.java.build;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDir;
import org.jake.JakeLog;
import org.jake.JakeZipper;

/**
 * Jar maker for the {@link JakeJavaBuild} template. This maker will get information from supplied java builder
 * to create relevant jars.
 * 
 * @author Jerome Angibaud
 */
public class JakeJavaPacker {

	private final JakeJavaBuild build;

	private final boolean fatJar;

	private final int compressionLevel;

	private final boolean includeVersion;

	private final boolean fullName;

	private final boolean checkSum;

	protected JakeJavaPacker(JakeJavaBuild buildBase) {
		this(buildBase, false, false, Deflater.DEFAULT_COMPRESSION, true, false);
	}

	protected JakeJavaPacker(JakeJavaBuild buildBase, boolean includeVersion, boolean fatJar, int compressionLevel, boolean fullName, boolean checkSum) {
		super();
		this.build = buildBase;
		this.fatJar = fatJar;
		this.compressionLevel = compressionLevel;
		this.includeVersion = includeVersion;
		this.fullName = fullName;
		this.checkSum = checkSum;
	}

	public static JakeJavaPacker of(JakeJavaBuild buildJava) {
		return new JakeJavaPacker(buildJava);
	}

	public JakeJavaPacker withFatJar(boolean fatJar) {
		return new JakeJavaPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	public JakeJavaPacker withCompression(int compressionLevel) {
		return new JakeJavaPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	public JakeJavaPacker withIncludeVersion(boolean includeVersion) {
		return new JakeJavaPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	public JakeJavaPacker withFullName(boolean fullName) {
		return new JakeJavaPacker(this.build, includeVersion, fatJar, compressionLevel, fullName, checkSum);
	}

	protected JakeJavaBuild build() {
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

	public File javadocFile() {
		return build().ouputDir(baseName() + "-javadoc.jar");
	}

	public void pack() {
		JakeLog.startln("Packaging module");
		JakeDir.of(build().classDir()).zip().to(jarFile(), compressionLevel).md5(checkSum);
		build().sourceDirs().and(build().resourceDirs()).zip().to(jarSourceFile(), compressionLevel);
		if (!build().skipTests && build().testClassDir().exists() && !JakeDir.of(build.testClassDir()).files().isEmpty()) {
			JakeZipper.of(build().testClassDir()).to(jarTestFile(), compressionLevel);
		}
		if (!build.testSourceDirs().files().isEmpty()) {
			build().testSourceDirs().and(build().testResourceDirs()).zip().to(jarTestSourceFile(), compressionLevel);
		}
		if (fatJar) {
			JakeDir.of(build().classDir()).zip().merge(build().depsFor(JakeJavaBuild.RUNTIME))
			.to(fatJarFile(), compressionLevel).md5(checkSum);
		}
		JakeLog.done();
	}


}
