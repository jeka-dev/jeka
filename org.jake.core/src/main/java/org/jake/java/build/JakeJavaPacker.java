package org.jake.java.build;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
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
public class JakeJavaPacker implements Cloneable {

	public static JakeJavaPacker.Builder builder(JakeJavaBuild build) {
		return JakeJavaPacker.of(build).builder();
	}

	public static JakeJavaPacker of(JakeJavaBuild build) {
		return new JakeJavaPacker(build);
	}

	private final JakeJavaBuild build;

	private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	private boolean includeVersion = false;

	private boolean fullName = false;

	private boolean checkSum = false;

	private boolean doJar = true;

	private boolean doTest = true;

	private boolean doSources = true;

	private boolean doFatJar = false;

	private List<Extra> extraActions = new LinkedList<Extra>();

	private JakeJavaPacker(JakeJavaBuild build) {
		this.build = build;
		this.doFatJar = build.fatJar;
	}

	public String baseName() {
		final String name = fullName ? build.projectFullName() : build.projectName();
		if (includeVersion) {
			return name + "-" + build.version();
		}
		return name;
	}

	public Builder builder() {
		return new JakeJavaPacker.Builder(this);
	}

	public File jarFile() {
		return build.ouputDir(baseName() + ".jar");
	}

	public File jarSourceFile() {
		return build.ouputDir(baseName() + "-sources.jar");
	}

	public File jarTestFile() {
		return build.ouputDir(baseName() + "-test.jar");
	}

	public File jarTestSourceFile() {
		return build.ouputDir(baseName() + "-test-sources.jar");
	}

	public File fatJarFile() {
		return build.ouputDir(baseName() + "-fat.jar");
	}

	public File javadocFile() {
		return build.ouputDir(baseName() + "-javadoc.jar");
	}

	public void pack() {
		JakeLog.startln("Packaging module");
		if (doJar) {
			JakeDir.of(build.classDir()).zip().to(jarFile(), compressionLevel).md5If(checkSum);
		}
		if (doSources) {
			build.sourceDirs().and(build.resourceDirs()).zip().to(jarSourceFile(), compressionLevel);
		}
		if (doTest && !build.skipTests && build.testClassDir().exists() && !JakeDir.of(build.testClassDir()).files().isEmpty()) {
			JakeZipper.of(build.testClassDir()).to(jarTestFile(), compressionLevel);
		}
		if (doTest && doSources && !build.testSourceDirs().files().isEmpty()) {
			build.testSourceDirs().and(build.testResourceDirs()).zip().to(jarTestSourceFile(), compressionLevel);
		}
		if (doFatJar) {
			JakeDir.of(build.classDir()).zip().merge(build.depsFor(JakeJavaBuild.RUNTIME))
			.to(fatJarFile(), compressionLevel).md5If(checkSum);
		}
		for (final Extra action : this.extraActions) {
			action.process(build);
		}
		JakeLog.done();
	}





	public interface Extra {

		public void process(JakeJavaBuild build);

	}

	public static class Builder {

		private final JakeJavaPacker packer;

		private Builder(JakeJavaPacker packer) {
			this.packer = packer.clone();
		}

		/**
		 * Compression of the archive files. Should be expressed with {@link Deflater} constants.
		 * Default is {@link Deflater#DEFAULT_COMPRESSION}.
		 */
		public Builder compressionLevel(int level) {
			packer.compressionLevel = level;
			return this;
		}

		/**
		 * True to include the version in the file names.
		 */
		public Builder includeVersion(boolean includeVersion) {
			packer.includeVersion = includeVersion;
			return this;
		}

		/**
		 * True means that the name of the archives will include the groupId of the artifact.
		 */
		public Builder fullName(boolean fullName) {
			packer.fullName = fullName;
			return this;
		}

		/**
		 * True to generate MD-5 check sum for archives.
		 */
		public Builder checkSum(boolean checkSum) {
			packer.checkSum = checkSum;
			return this;
		}

		/**
		 * True to generate a jar file containing both classes and resources.
		 */
		public Builder doJar(boolean doJar) {
			packer.doJar = doJar;
			return this;
		}

		public Builder doTest(Boolean doTest) {
			packer.doTest = doTest;
			return this;
		}

		public Builder doSources(Boolean doSources) {
			packer.doSources = doSources;
			return this;
		}

		public Builder doFatJar(Boolean doFatJar) {
			packer.doFatJar = doFatJar;
			return this;
		}

		public Builder extraAction(Extra extra) {
			packer.extraActions.add(extra);
			return this;
		}

		public JakeJavaPacker build() {
			return packer.clone();
		}

	}

	@Override
	public JakeJavaPacker clone() {
		JakeJavaPacker clone;
		try {
			clone = (JakeJavaPacker) super.clone();
		} catch (final CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.extraActions = new LinkedList<JakeJavaPacker.Extra>(this.extraActions);
		return clone;
	}

}
