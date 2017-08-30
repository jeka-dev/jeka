package org.jerkar;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.project.JkArtifactFileId;
import org.jerkar.api.project.java.JkJarProject;
import org.jerkar.api.project.java.JkJavaCompileVersion;
import org.jerkar.api.system.JkLog;

import java.io.File;

// Experimental
class _CoreProject extends JkJarProject {

    static JkArtifactFileId DISTRIB_FILE_ID = JkArtifactFileId.of("distrib", "zip");

    static JkArtifactFileId ALL_FAT_FILE_ID = JkArtifactFileId.of("all", "jar");

    private File distribFolder;

    public _CoreProject(File baseDir) {
        super(baseDir);
        this.distribFolder = new File(baseDir, "jerkar-distrib");
        this.setCompileVersion(JkJavaCompileVersion.V8);
    }

    @Override
    public JkArtifactFileId.JkArtifactFileIds extraArtifactFileIds() {
        return SOURCES_FILE_ID.and(DISTRIB_FILE_ID, ALL_FAT_FILE_ID, JAVADOC_FILE_ID);
    }

    @Override
    public void doArtifactFile(JkArtifactFileId artifactFileId) {
        if (artifactFileId.equals(DISTRIB_FILE_ID)) {
            doDistrib();
        } else if (artifactFileId.equals(ALL_FAT_FILE_ID)) {
            doFatJar();
        } else {
            this.doArtifactFile(artifactFileId);
        }
    }

    private void doDistrib() {
        packSourceJar();
        this.doArtifactFile(JAVADOC_FILE_ID);
        this.doArtifactFile(mainArtifactFileId());
        makeDistrib();
    }

    private void doFatJar() {
        commonBuild();
        packFatJar(ALL_FAT_FILE_ID.classifier());
    }

    private void makeDistrib() {
        File distripZipFile = this.getArtifactFile(DISTRIB_FILE_ID);
        final JkFileTree distrib = JkFileTree.of(distribFolder);
        JkLog.startln("Creating distrib " + distripZipFile.getPath());
        distrib.importFiles(root().file("../LICENSE"));
        distrib.importDirContent(root().file("src/main/dist"));
        distrib.importDirContent(root().file("src/main/java/META-INF/bin"));
        distrib.importFiles(this.getArtifactFile(mainArtifactFileId()));
        distrib.go("libs-sources").importDirContent(root().file("build/libs-sources"))
                .importFiles(this.getArtifactFile(SOURCES_FILE_ID));
        distrib.go("libs-javadoc").importFiles(this.getArtifactFile(JAVADOC_FILE_ID));
        distrib.zip().with(JkZipper.JkCompressionLevel.BEST_COMPRESSION).to(distripZipFile);
    }
}
