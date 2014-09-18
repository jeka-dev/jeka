package org.jake.verify.sonar;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lombok.Value;
import lombok.experimental.Builder;

import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJava;
import org.jake.java.JakeClasspath;
import org.jake.java.JakeJavaProcess;


@Value
@Builder(fluent=true, builderClassName="Builder")
public class JakeSonar {

	public enum AnalyseMode {
		analysis, preview, incremental
	}

	File projectDir;

	Iterable<File> sources;

	String projectKey;

	String projectName;

	String projectVersion;

	String language;

	Iterable<File> binaries;

	Iterable<File> tests;

	Iterable<File> libraries;

	String sourceEncoding;

	String branch;

	String profile;

	boolean skipDesign;

	public static JakeSonar of(JakeBuildJava build) {

		// @formatter:off
		return builder()
				.projectDir(build.baseDir().root())
				.sources(build.editedSourceDirs().baseDirs())
				.binaries(JakeClasspath.of(build.classDir()))
				.tests(build.testSourceDirs().baseDirs())
				.libraries(build.deps().compile())
				.sourceEncoding(build.sourceEncoding())
				.projectKey(build.groupName())
				.projectName(build.projectName())
				.projectVersion(build.version())
				.language("java")
				.build();
		// @formatter:on
	}

	public void launch() {
		JakeLog.startAndNextLine("Launching Sonar analysis");
		javaProcess().startAndWaitFor("org.sonar.runner.Main");
		JakeLog.done();
	}

	private JakeJavaProcess javaProcess() {
		final JakeJavaProcess result = JakeJavaProcess.of()
				.withClasspath(jarRunner())
				.andOptions(toProperties());
		return result;
	}


	private List<String> toProperties() {
		final List<String> result = new LinkedList<String>();
		append(result, "sourceEncoding", sourceEncoding);
		append(result, "projectKey", projectKey);
		append(result, "projectName", projectName);
		append(result, "projectVersion", projectVersion);
		append(result, "language", language);
		append(result, "sources", toPaths(sources));
		append(result, "binaries", toPaths(binaries));
		append(result, "tests", toPaths(tests));
		append(result, "libraries", toString(libraries));
		append(result, "profile", profile);
		append(result, "projectBaseDir", projectDir.getAbsoluteFile());
		append(result, "branch", branch);
		append(result, "skipDesign", skipDesign);
		return result;
	}

	private void append(List<String> result, String key, Object value) {
		if (value != null) {
			result.add("-Dsonar."+key+"="+value);
		}
	}

	private String toPaths(Iterable<File> files) {
		final Iterator<File> it = files.iterator();
		final StringBuilder result = new StringBuilder();
		while (it.hasNext()) {
			result.append(JakeUtilsFile.getRelativePath(projectDir, it.next()));
			if (it.hasNext()) {
				result.append(",");
			}
		}
		return result.toString();
	}

	private static String toString(Iterable<File> files) {
		final Iterator<File> it = files.iterator();
		final StringBuilder result = new StringBuilder();
		while (it.hasNext()) {
			result.append(it.next().getAbsolutePath());
			if (it.hasNext()) {
				result.append(",");
			}
		}
		return result.toString();
	}

	private File jarRunner() {
		final File localJar = new File(profile, "build/libs/sonar-runner/sonar-runner.jar");
		final File sharedJar = new File(JakeLocator.optionalLibsDir(), "sonar-runner.jar");
		final File jar = localJar.exists() ? localJar : sharedJar;
		if (!jar.exists()) {
			throw new IllegalStateException("No sonar-runner.jar found neither in " + localJar.getAbsolutePath()
					+ " nor in " + sharedJar.getAbsolutePath() );
		}
		return jar;
	}






}
