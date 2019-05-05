package org.jerkar.samples;

import com.google.common.base.MoreObjects;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImport;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;

import java.io.IOException;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.jerkar.api.depmanagement.JkPopularModules.*;

/**
 * This build demonstrates how to use 3rd party dependencies into your build class.
 * 
 * @author Jerome Angibaud
 * @formatter:off
 */
@JkImport("commons-httpclient:commons-httpclient:3.1")
@JkImport("com.google.guava:guava:21.0")
public class HttpClientTaskBuild extends JkRun {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    @Override
    protected void setup() {
        javaPlugin.getProject().addDependencies(dependencies());
    }


    private static JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and(GUAVA, "21.0")
                .and(JAVAX_SERVLET_API, "3.1.0", PROVIDED)
                .and(JUNIT, "4.11", TEST)
                .and(MOCKITO_ALL, "1.10.19", TEST);
    }

    @JkDoc("Performs some load test using http client")
    public void seleniumLoadTest() throws IOException {
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://my.url");
        client.executeMethod(getMethod);
        client = MoreObjects.firstNonNull(client, client); // senseless but just to illustrate we can use Guava
        // ....
    }

}
