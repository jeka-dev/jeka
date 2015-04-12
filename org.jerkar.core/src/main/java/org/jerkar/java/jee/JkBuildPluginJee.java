package org.jerkar.java.jee;

import java.io.File;

import org.jerkar.JkBuild;
import org.jerkar.JkLog;
import org.jerkar.JkOption;
import org.jerkar.java.build.JkJavaBuild;
import org.jerkar.java.build.JkJavaBuildPlugin;
import org.jerkar.java.build.JkJavaPacker;
import org.jerkar.java.build.JkJavaPacker.Extra;

public class JkBuildPluginJee extends JkJavaBuildPlugin {

	private JkJavaBuild build;

	@JkOption("Location of the webapp sources (containing WEB-INF dir along static resources).")
	public String webappSrc = "src/main/webapp";

	@JkOption("True to produce a test jar containing test classes.")
	public boolean testJar = false;

	@JkOption("True to produce a regular jar containing classes and resources.")
	public boolean regularJar = false;

	@Override
	public void configure(JkBuild build) {
		this.build = (JkJavaBuild) build;
	}

	public File warFile() {
		return this.build.ouputDir(build.packer().baseName()+".war");
	}

	private File webappSrcFile() {
		return build.baseDir(webappSrc);
	}

	@Override
	protected JkJavaPacker alterPacker(final JkJavaPacker packer) {
		final JkJavaPacker.Builder builder = packer.builder().doJar(regularJar)
				.doTest(testJar).doFatJar(false);
		if (webappSrcFile().exists()) {
			builder.extraAction(new Extra() {

				@Override
				public void process(JkJavaBuild build) {
					JkLog.startln("Creating war file");
					final File dir = build.ouputDir(packer.baseName() + "-war");
					JkJeePacker.of(build).war(webappSrcFile(), dir, warFile());
					JkLog.done();
				}
			});
		} else {
			JkLog.warn("No webapp source found at " + webappSrcFile().getPath());
		}
		return builder.build();

	}



}
