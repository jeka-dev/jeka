package org.jerkar.builtins.jee;

import java.io.File;
import java.io.IOException;

import org.jerkar.JkBuild;
import org.jerkar.JkBuildResolver;
import org.jerkar.JkLog;
import org.jerkar.JkOption;
import org.jerkar.java.build.JkJavaBuild;
import org.jerkar.java.build.JkJavaBuildPlugin;
import org.jerkar.java.build.JkJavaPacker;
import org.jerkar.java.build.JkJavaPacker.Extra;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;

public class JkBuildPluginWar extends JkJavaBuildPlugin {

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
	protected void enhanceScaffold() {
		final File webInf = this.build.baseDir(webappSrc + "/WEB-INF");
		webInf.mkdirs();
		try {
			final File webxml = new File(webInf, "web.xml");
			if (!webxml.exists()) {
				JkLog.info("Create web.xml");
				webxml.createNewFile();
				JkUtilsFile.writeString(webxml, "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" version=\"2.5\">\n", true);
				JkUtilsFile.writeString(webxml, "</web-app>", true);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final File defaultBuild = new File(this.build.baseDir(JkBuildResolver.BUILD_SOURCE_DIR), this.build.groupName() + "/Build.java");
		defaultBuild.delete();
		JkUtilsFile.createFileIfNotExist(defaultBuild);
		String content = JkUtilsIO.read(JkBuildPluginWar.class.getResource("Build.java_sample"));
		content = content.replace("__groupName__", this.build.groupName());
		JkUtilsFile.writeString(defaultBuild, content, false);
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
