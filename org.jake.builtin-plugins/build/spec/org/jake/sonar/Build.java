package org.jake.sonar;

import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaPacker;
import org.jake.utils.JakeUtilsFile;

public class Build extends JakeJavaBuild {
	
	@Override
	public void base() {
		super.base();
		pack();		
	}
	
	@Override
	protected JakeDependencies dependencies() {
		return super.dependencies().
				andFiles(PROVIDED, baseDir("../jake/build/output/classes"));
	}
	
	public static void main(String[] args) {
		new Build().base();
	}
	
	
		

}
