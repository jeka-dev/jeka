package org.jake.java.jee;

import java.io.File;

import org.jake.JakeDir;
import org.jake.java.build.JakeJavaBuild;

/**
 * War and Ear maker for {@link JakeJavaBuild}. This maker will get information from supplied java builder
 * to create relevant jars.
 * 
 * @author Jerome Angibaud
 */
public class JakeJeePacker {

	private final JakeJavaBuild build;

	private JakeJeePacker(JakeJavaBuild build) {
		super();
		this.build = build;
	}

	public void packWar(File webappSrc, File warDirDest, File warFileDest) {
		final JakeDir dir = JakeDir.of(warDirDest).copyInDirContent(webappSrc)
				.sub("WEB-INF/classes").copyInDirContent(build.classDir())
				.sub("../libs").copyInFiles(build.depsFor(JakeJavaBuild.RUNTIME));
		dir.zip().to(warFileDest);
	}

	public void packEar(Iterable<File> warFiles, File earSrc, File dest) {

	}









}
