package dev.jeka.core.samples;

import com.google.common.base.MoreObjects;
import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.*;
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

    @JkInject
    private ProjectKBean projectKBean;

    @JkDoc("Performs some load test using http client")
    public void seleniumLoadTest() throws IOException {
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://my.url");
        client.executeMethod(getMethod);
        client = MoreObjects.firstNonNull(client, client); // senseless but just to illustrate we can use Guava
        // ....
    }

    @JkDoc("Cleans then packs the project")
    public void cleanPack() {
        cleanOutput();
        projectKBean.pack();
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setModuleId("dev.jeka.samples:war-project")
                .setVersion("1.0-SNAPSHOT")
                .setJvmTargetVersion(JkJavaVersion.V8)
                .compilation.layout.emptySources().addSources("src/main/javaweb");
        project.testing.setSkipped(true);

        project.flatFacade.dependencies.compile.modify(deps -> deps
                .and("com.google.guava:guava:30.0-jre")
                .and("javax.servlet:javax.servlet-api:4.0.1"));
        project.flatFacade.dependencies.runtime
                .remove("javax.servlet:javax.servlet-api");
        JkJ2eWarProjectAdapter.of().configure(project);
    }

    public static void main(String[] args) {
        JkInit.kbean(ThirdPartyDependenciesKBean.class, args).cleanPack();
    }

}
