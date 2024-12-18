package dev.jeka.core.samples;

import com.google.common.base.MoreObjects;
import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

import static dev.jeka.core.api.depmanagement.JkPopularLibs.*;

/**
 * This build demonstrates how to use 3rd party dependencies into your build class.
 * 
 * @author Jerome Angibaud
 */
@JkDep("commons-httpclient:commons-httpclient:3.1")
@JkDep("com.google.guava:guava:21.0")
public class ThirdPartyDependenciesKBean extends KBean {

    ProjectKBean projectKBean = load(ProjectKBean.class);

    @Override
    protected void init() {
        projectKBean.project.flatFacade.dependencies.compile
                .add(JkPopularLibs.JAVAX_SERVLET_API.toCoordinate("3.1.0"))
                .add(JkPopularLibs.GUAVA.toCoordinate("30.0-jre"));
        projectKBean.project.flatFacade.dependencies.runtime
                .remove(JkPopularLibs.JAVAX_SERVLET_API.getDotNotation());
        projectKBean.project.flatFacade.dependencies.test
                .add(SimpleProjectKBean.JUNIT5)
                .add(JkPopularLibs.MOCKITO_ALL.toCoordinate("1.10.19"));
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
        cleanOutput();
        projectKBean.pack();
    }

    public static void main(String[] args) {
        JkInit.kbean(ThirdPartyDependenciesKBean.class, args).cleanPack();
    }

}
