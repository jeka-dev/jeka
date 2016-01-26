package org.jerkar.tool.builtins.javabuild.jee;

import java.io.File;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * War and Ear maker for {@link JkJavaBuild}. This maker will get information
 * from supplied java builder to create relevant jars.
 *
 * @author Jerome Angibaud
 */
class JeePacker {

    public static JeePacker of(JkJavaBuild build) {
        return new JeePacker(build);
    }

    private final JkJavaBuild build;

    private JeePacker(JkJavaBuild build) {
        super();
        this.build = build;
    }

    public void war(File webappSrc, File warDirDest, File warFileDest) {
        if (!new File(webappSrc, "WEB-INF/web.xml").exists()) {
            throw new IllegalStateException("The directory " + webappSrc.getPath()
                    + " does not contains WEB-INF" + File.separator + "web.xml file");
        }
        final JkPath path = build.depsFor(JkJavaBuild.RUNTIME);
        JkFileTree.of(warDirDest).importDirContent(webappSrc)
        .from("WEB-INF/classes").importDirContent(build.classDir()).from("../lib")
        .importFiles(path);
        JkFileTree.of(warDirDest).zip().to(warFileDest);
    }

    public void ear(Iterable<File> warFiles, File earSrc, File destDir, File destFile) {
        JkFileTree.of(destDir).importDirContent(earSrc).importFiles(warFiles).zip().to(destFile);
    }

}
