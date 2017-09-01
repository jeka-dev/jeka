package org.jerkar;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.project.JkArtifactFileId;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaCompileVersion;

import java.io.File;

// Experimental
public class _CoreProject extends JkJavaProject {

    static JkArtifactFileId DISTRIB_FILE_ID = JkArtifactFileId.of("distrib", "zip");

    //static JkArtifactFileId ALL_FAT_FILE_ID = JkArtifactFileId.of("all", "jar");

    public File distribFolder;


    public _CoreProject(File baseDir) {
        super(baseDir);
        this.setOutLayout(this.getOutLayout().withOutputDir(new File(baseDir, "build/output2")));
        this.setCompileVersion(JkJavaCompileVersion.V8);
        this.addArtifactFile(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = new File(this.getOutLayout().outputDir(), "distrib");
    }

    private void doDistrib() {
        File distripZipFile = this.artifactFile(DISTRIB_FILE_ID);
        this.doArtifactFilesIfNecessary(SOURCES_FILE_ID, JAVADOC_FILE_ID, mainArtifactFileId());
        final JkFileTree distrib = JkFileTree.of(distribFolder);
        distrib.importFiles(root().file("../LICENSE"));
        distrib.importDirContent(root().file("src/main/dist"));
        distrib.importDirContent(root().file("src/main/java/META-INF/bin"));
        distrib.importFiles(this.artifactFile(mainArtifactFileId()));
        distrib.go("libs-sources")
                .importFiles(root().go("build/libs-sources").include("apache-ivy*.jar"))
                .importFiles(this.artifactFile(SOURCES_FILE_ID));
        distrib.go("libs-javadoc").importFiles(this.artifactFile(JAVADOC_FILE_ID));
        distrib.zip().with(JkZipper.JkCompressionLevel.BEST_COMPRESSION).to(distripZipFile);
    }
}
