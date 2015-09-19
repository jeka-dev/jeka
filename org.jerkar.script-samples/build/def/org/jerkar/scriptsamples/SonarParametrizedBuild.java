package org.jerkar.scriptsamples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.plugins.jacoco.JkBuildPluginJacoco;
import org.jerkar.plugins.sonar.JkBuildPluginSonar;
import org.jerkar.plugins.sonar.JkSonar;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * This build clean, compile,test launch sonar analyse by default.
 *
 * @author Jerome Angibaud
 */
public class SonarParametrizedBuild extends JkJavaBuild {
	
	@JkDoc("Sonar server environment")
	protected SonarEnv sonarEnv = SonarEnv.DEV;
	
	@Override
	protected void init() {
		JkBuildPluginSonar sonarPlugin = new JkBuildPluginSonar()
			.prop(JkSonar.HOST_URL, sonarEnv.url)
			.prop(JkSonar.BRANCH, "myBranch");
		JkBuildPluginJacoco jacocoPlugin = new JkBuildPluginJacoco();
		this.plugins.activate(sonarPlugin, jacocoPlugin);
	}
	
	@Override  
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0")  
			.on(JUNIT, "4.11").scope(TEST)
		.build();
	}
	
	@Override
	public void doDefault() {
		doUnitTest();
		
		// Verify method has extension point hooked by sonar plugin
		// so when sonar plugin is activated, JkBuild#verify 
		// launch the #verfy method on all activated plugins
		verify(); 
	}
	
	public enum SonarEnv {
		DEV("dev.myhost:81"),
		QA("qa.myhost:81"),
		PROD("prod.myhost:80");
		
		public final String url;
		
		SonarEnv(String url) {
			this.url = url;
		}
	}
}
