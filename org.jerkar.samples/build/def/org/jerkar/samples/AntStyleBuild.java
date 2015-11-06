package org.jerkar.samples;

import java.io.File;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;

/**
 * Equivalent to http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html
 * 
 * @author Jerome Angibaud
 */
public class AntStyleBuild extends JkBuild {
	
	File src = file("src/main/java");
	File buildDir = file("build/output");
	File classDir = new File(buildDir, "classes");
	File jarFile = new File(buildDir, "jar/" + this.baseDir().root().getName() + ".jar");
	JkClasspath classpath = JkClasspath.of(baseDir().include("libs/**/*.jar"));
	File reportDir = new File(buildDir, "junitRreport");
	
	
	
	@Override
	public void doDefault() {
		clean();run();
		
	}
	
	public void compile() {
		JkJavaCompiler.ofOutput(classDir).withClasspath(classpath).andSourceDir(src).compile();
		JkFileTree.of(src).exclude("**/*.java").copyTo(classDir);
	}
	
	public void jar() {
		compile();
		JkManifest.empty().addMainClass("org.jerkar.samples.RunClass").writeToStandardLocation(classDir);
		JkFileTree.of(classDir).zip().to(jarFile);
	}	
	
	public void run() {
		jar();
		JkJavaProcess.of().withWorkingDir(jarFile.getParentFile())
			.andClasspath(classpath).runJarSync(jarFile);
	}
	
	public void cleanBuild() {
		clean();jar();
	}
	
	public void junit() {
		jar();
		JkUnit.ofFork(classpath.and(jarFile))
				.withClassesToTest(JkFileTree.of(classDir).include("**/*Test.class"))
				.withReportDir(reportDir)
				.withReport(JunitReportDetail.FULL)
				.run();
	}
	
	/*
	 * This part is specific to Maven publishing and does not exist in the original helloWorld ANT file 
	 */
	@JkDoc("Redefine this value to set your own publish repository.")
	protected String publishRepo = "http://my/publish/repo";
	
	protected String pgpPrivateRingFile = "/usr/myUser/pgp/pub";
	
	protected String pgpPassword = "mypPgpPassword";
	
	public void publish() {
		JkPgp pgp = JkPgp.ofSecretRing(new File(pgpPrivateRingFile), pgpPassword);
		JkPublishRepo repo = JkRepo.maven(publishRepo)
				.withCredential("myRepoUserName", "myRepoPassword")
				.asPublishRepo()
				.withUniqueSnapshot(false)
				.withSigner(pgp)
				.andSha1Md5Checksums();
				
		JkVersionedModule versionedModule = JkVersionedModule.of("myGroup:myName", "0.2.1");
		
		// Optinal : if you need to add metadata in the generated pom
		JkMavenPublicationInfo info = JkMavenPublicationInfo
				.of("my project", "my description", "http://myproject.github")
				.withScm("http://scm/url/connection")
				.andApache2License()
				.andGitHubDeveloper("myName", "myName@provider.com");				
		
		// Optional : if you want publish sources
		File srcZip = ouputDir("src.zip");
		JkZipper.of(this.src).to(srcZip);
		
		JkMavenPublication publication = JkMavenPublication.of(jarFile).with(info).and(srcZip, "sources");
		JkPublisher.of(repo).publishMaven(versionedModule, publication, JkDependencies.on());
	}
	
	public static void main(String[] args) {
		new AntStyleBuild().doDefault();
	}
	
	
	
}
