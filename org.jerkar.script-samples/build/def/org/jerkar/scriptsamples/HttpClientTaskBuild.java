package org.jerkar.scriptsamples;

import static org.jerkar.builtins.javabuild.JkPopularModules.GUAVA;
import static org.jerkar.builtins.javabuild.JkPopularModules.JAVAX_SERVLET_API;
import static org.jerkar.builtins.javabuild.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.builtins.javabuild.JkPopularModules.JUNIT;
import static org.jerkar.builtins.javabuild.JkPopularModules.MOCKITO_ALL;

import org.apache.commons.httpclient.HttpClient;
import org.jerkar.JkDoc;
import org.jerkar.JkImport;
import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkDependencies;

/**
 * This build demonstrate how to use 3rd party dependencies in your build.
 * 
 * @author Jerome Angibaud
 */
@JkImport("commons-httpclient:commons-httpclient:3.1")
public class HttpClientTaskBuild extends JkJavaBuild {
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0")
			.on(JERSEY_SERVER, "1.19")
			.on("com.orientechnologies:orientdb-client:2.0.8")
			.on(JAVAX_SERVLET_API, "2.5").scope(PROVIDED)
			.on(JUNIT, "4.11").scope(TEST)
			.on(MOCKITO_ALL, "1.9.5").scope(TEST)
		.build();
	}
	
	@JkDoc("Performs some load test using http client")
	public void seleniumLoadTest() {
		HttpClient client = new HttpClient();
		// ....
	}
	
	public void doDefault() {
		super.doDefault();
		seleniumLoadTest();
	}
	
	
}
