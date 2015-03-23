package org.jake.java.jee;

import java.io.File;

import org.jake.JakeBuild;
import org.jake.JakeLog;
import org.jake.JakeOption;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.build.JakeJavaPacker;

public class JakeBuildPluginJee extends JakeJavaBuildPlugin {

	private JakeJavaBuild build;

	@JakeOption("Location of the webapp sources (containing WEB-INF dir along static resources).")
	public String webappSrc = "src/main/webapp";

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
	protected JakeJavaPacker enhance(final JakeJavaPacker packer) {
		JakeJavaPacker result = packer;
		if (webappSrcFile().exists()) {
			result = result.withExtraAction(new Runnable() {

				@Override
				public void run() {
					final File dir = build.ouputDir(packer.baseName() + "-war");
					JakeJeePacker.of(build).war(webappSrcFile(), dir, warFile());
				}

			});
		} else {
			JakeLog.warn("No webapp source found at " + webappSrcFile().getPath());
		}
		return result;

	}



}
