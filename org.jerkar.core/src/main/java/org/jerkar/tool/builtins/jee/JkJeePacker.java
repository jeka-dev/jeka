package org.jerkar.tool.builtins.jee;

import java.io.File;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild;


/**
 * War and Ear maker for {@link JkJavaBuild}. This maker will get information from supplied java builder
 * to create relevant jars.
 * 
 * @author Jerome Angibaud
 */
public class JkJeePacker {

	public static JkJeePacker of(JkJavaBuild build) {
		return new JkJeePacker(build);
	}

	private final JkJavaBuild build;

	private JkJeePacker(JkJavaBuild build) {
		super();
		this.build = build;
	}

	public void war(File webappSrc, File warDirDest, File warFileDest) {
		if (! new File(webappSrc, "WEB-INF/web.xml").exists()) {
			throw new IllegalStateException("The directory " + webappSrc.getPath()
					+ " does not contains WEB-INF" + File.separator + "web.xml file");
		}
		final JkPath path = build.depsFor(JkJavaBuild.RUNTIME);
		final JkFileTree dir = JkFileTree.of(warDirDest).importDirContent(webappSrc)
				.from("WEB-INF/classes").importDirContent(build.classDir())
				.from("../lib").importFiles(path);
		dir.zip().to(warFileDest);
	}

	public void ear(Iterable<File> warFiles, File earSrc, File destDir, File destFile) {
		JkFileTree.of(destDir).importDirContent(earSrc).importFiles(warFiles).zip().to(destFile);
	}

}
