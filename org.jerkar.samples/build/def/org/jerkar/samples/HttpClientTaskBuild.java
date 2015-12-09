package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JAVAX_SERVLET_API;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;
import static org.jerkar.api.depmanagement.JkPopularModules.MOCKITO_ALL;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImport;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * This build demonstrates how to use 3rd party dependencies in your build.
 * 
 * @author Jerome Angibaud
 * @fprmatter:off
 */
@JkImport({"commons-httpclient:commons-httpclient:3.1"})
public class HttpClientTaskBuild extends JkJavaBuild {

	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
				.on(GUAVA, "18.0")
				.on(JAVAX_SERVLET_API, "2.5", PROVIDED)
				.on(JUNIT, "4.11", TEST)
				.on(MOCKITO_ALL, "1.9.5", TEST).build();
	}

	@JkDoc("Performs some load test using http client")
	public void seleniumLoadTest() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		GetMethod getMethod = new GetMethod("http://my.url");
		client.executeMethod(getMethod);
		// ....
	}

}
