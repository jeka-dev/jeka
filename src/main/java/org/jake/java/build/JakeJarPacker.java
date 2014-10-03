package org.jake.java.build;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDir;
import org.jake.JakeLog;
import org.jake.JakeZip;
import org.jake.depmanagement.JakeScope;

/**
 * Jar maker for the {@link JakeBuildJava} template. This maker will get information from supplied java builder
 * to create relevant jars.
 * 
 * @author Jerome Angibaud
 */
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
		JakeDir.of(build().classDir()).zip().to(jarFile(), compressionLevel).md5(checkSum);
		build().sourceDirs().and(build().resourceDirs()).zip().to(jarSourceFile(), compressionLevel);
		if (!build().skipTests()) {
			JakeZip.of(build().testClassDir()).to(jarTestFile(), compressionLevel);
		}
		build().testSourceDirs().and(build().testResourceDirs()).zip().to(jarTestSourceFile(), compressionLevel);
		if (fatJar) {
			JakeDir.of(build().classDir()).zip().merge(build().deps(JakeScope.RUNTIME))
			.to(fatJarFile(), compressionLevel).md5(checkSum);
		}
		JakeLog.done();
	}


}
