package org.jake.java.jee;

import java.io.File;

import org.jake.JakeBuild;
import org.jake.JakeLog;
import org.jake.JakeOption;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.build.JakeJavaPacker;
import org.jake.java.build.JakeJavaPacker.Extra;

public class JakeBuildPluginJee extends JakeJavaBuildPlugin {

	private JakeJavaBuild build;

	@JakeOption("Location of the webapp sources (containing WEB-INF dir along static resources).")
	public String webappSrc = "src/main/webapp";

	@JakeOption("True to produce a test jar containing test classes.")
	public boolean testJar = false;

	@JakeOption("True to produce a regular jar containing classes and resources.")
	public boolean regularJar = false;

	@Override
	public void configure(JakeBuild build) {
		this.build = (JakeJavaBuild) build;
	}

	public File warFile() {
		return this.build.ouputDir(build.packer().baseName()+".war");
	}

	private File webappSrcFile() {
		return build.baseDir(webappSrc);
	}

	@Override
	protected JakeJavaPacker alterPacker(final JakeJavaPacker packer) {
		final JakeJavaPacker.Builder builder = packer.builder().doJar(regularJar)
				.doTest(testJar).doFatJar(false);
		if (webappSrcFile().exists()) {
			builder.extraAction(new Extra() {

				@Override
				public void process(JakeJavaBuild build) {
					JakeLog.startln("Creating war file");
					final File dir = build.ouputDir(packer.baseName() + "-war");
					JakeJeePacker.of(build).war(webappSrcFile(), dir, warFile());
					JakeLog.done();
				}
			});
		} else {
			JakeLog.warn("No webapp source found at " + webappSrcFile().getPath());
		}
		return builder.build();

	}



}
