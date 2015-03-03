package org.jake.plugins.jacoco;

import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class JakeocoBuild extends JakeJavaBuild {
	
	public final JakeJavaBuild core = relativeProject(JakeJavaBuild.class, "../org.jake.core");
	
	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies.onProject(PROVIDED, core , core.packer().jarFile());
	}
	
	
}
