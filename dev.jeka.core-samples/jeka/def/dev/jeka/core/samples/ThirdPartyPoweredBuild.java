package dev.jeka.core.samples;

import com.google.common.base.MoreObjects;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

import static dev.jeka.core.api.depmanagement.JkPopularModules.*;

/**
 * This build demonstrates how to use 3rd party dependencies into your build class.
 * 
 * @author Jerome Angibaud
 * @formatter:off
 */
@JkDefClasspath("commons-httpclient:commons-httpclient:3.1")
@JkDefClasspath("com.google.guava:guava:21.0")
public class ThirdPartyPoweredBuild extends JkClass {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    @Override
    protected void setup() {
        javaPlugin.getProject().simpleFacade()
            .addCompileDependencies(JkDependencySet.of()
                .and(JAVAX_SERVLET_API.version("3.1.0"))
                .and(GUAVA.version("21.0")))
            .setRuntimeDependencies(compileDeps -> compileDeps
                .minus(JAVAX_SERVLET_API))
            .addTestDependencies(JkDependencySet.of()
                .and(JUNIT.version("4.13"))
                .and(MOCKITO_ALL.version("1.10.19")));
    }

    @JkDoc("Performs some load test using http client")
    public void seleniumLoadTest() throws IOException {
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://my.url");
        client.executeMethod(getMethod);
        client = MoreObjects.firstNonNull(client, client); // senseless but just to illustrate we can use Guava
        // ....
    }

    public void cleanPack() {
        clean();
        javaPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(ThirdPartyPoweredBuild.class, args).cleanPack();
    }

}
