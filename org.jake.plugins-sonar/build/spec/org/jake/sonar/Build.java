package org.jake.sonar;

import org.jake.JakeJavaCompiler;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class Build extends JakeJavaBuild {
		
	@Override
	protected JakeDependencies dependencies() {
		final JakeJavaBuild core = relativeProject(JakeJavaBuild.class, "../org.jake.core");
		return JakeDependencies.onProject(PROVIDED, core, core.classDir());
	}
	
	@Override
	public String sourceJavaVersion() {
		return JakeJavaCompiler.V6;
	}	
		

}
