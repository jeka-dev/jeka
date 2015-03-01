package org.jake.sonar;

import org.jake.JakeJavaCompiler;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class Build extends JakeJavaBuild {
		
	@Override
	protected JakeDependencies dependencies() {
		final JakeJavaBuild coreBuild = relativeProject(JakeJavaBuild.class, "../orj.jake.core");
		return JakeDependencies.onProject(PROVIDED, coreBuild, coreBuild.classDir());
	}
	
	@Override
	public String sourceJavaVersion() {
		return JakeJavaCompiler.V6;
	}	
		

}
