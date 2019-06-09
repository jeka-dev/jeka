package dev.jeka.core.api.depmanagement;

import java.io.File;
import java.io.IOException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;

@SuppressWarnings("javadoc")
public class SampleIvyRunner {

    public void retrieve() {
        final IBiblioResolver dependencyResolver = new IBiblioResolver();
        dependencyResolver
        .setRoot("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured");
        dependencyResolver.setM2compatible(true);
        dependencyResolver.setUseMavenMetadata(true);
        dependencyResolver.setName("nexus"); // Name is necessary to avoid NPE

        final IvySettings ivySettings = new IvySettings();
        ivySettings.addResolver(dependencyResolver);
        ivySettings.setDefaultResolver("nexus"); // Setting a default resolver
        // is necessary

        final Ivy ivy = Ivy.newInstance(ivySettings);
        ivy.getLoggerEngine().setDefaultLogger(new DefaultMessageLogger(Message.MSG_DEBUG));

        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance("mygroupId",
                "myartifactId-envelope", "myversion");

        final ModuleRevisionId dependee = ModuleRevisionId.newInstance("org.springframework",
                "spring-jdbc", "3.0.0.RELEASE");
        // final ModuleRevisionId dependee =
        // ModuleRevisionId.newInstance("org.hibernate",
        // "hibernate-core", "3.6.10.Final");

        // 1st create an ivy module (this always(!) has a "default"
        // configuration already)
        final DefaultModuleDescriptor moduleDescriptor = DefaultModuleDescriptor
                .newDefaultInstance(thisModuleRevisionId);

        // don't go transitive here, if you want the single artifact
        final boolean transitive = true;
        final DefaultDependencyDescriptor dependencyDescriptor = new DefaultDependencyDescriptor(
                moduleDescriptor, dependee, false, false, transitive);

        // map to master to just get the code jar. See generated ivy module xmls
        // from maven repo
        // on how configurations are mapped into ivy. Or check
        // e.g.
        // http://lightguard-jp.blogspot.de/2009/04/ivy-configurations-when-pulling-from.html
        // dependencyDescriptor.addDependencyConfiguration("default", "master");

        // To get more than 1 artifact i need to declare "compile" and not
        // "master"
        dependencyDescriptor.addDependencyConfiguration("default", "compile");

        moduleDescriptor.addDependency(dependencyDescriptor);

        // now resolve
        final ResolveOptions resolveOptions = new ResolveOptions()
        .setConfs(new String[] { "default" });
        resolveOptions.setTransitive(transitive);
        ResolveReport reportResolver;
        try {
            reportResolver = ivy.resolve(moduleDescriptor, resolveOptions);
        } catch (final Exception e1) {
            throw new RuntimeException(e1);
        }
        if (reportResolver.hasError()) {
            System.out
            .println("*************************************************************************");
            System.out.println(reportResolver);

            throw new RuntimeException(reportResolver.getAllProblemMessages().toString());
        }
        for (final ArtifactDownloadReport artifactDownloadReport : reportResolver
                .getAllArtifactsReports()) {
            System.out.println("*********************************"
                    + artifactDownloadReport.getLocalFile());

        }

        final String filePattern = new File("jerkar/output/downloaded-libs").getAbsolutePath()
                + "/[artifact](-[classifier]).[ext]";
        final RetrieveOptions retrieveOptions = new RetrieveOptions()
        .setConfs(new String[] { "default" });
        try {
            ivy.retrieve(moduleDescriptor.getModuleRevisionId(), filePattern, retrieveOptions);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        new SampleIvyRunner().retrieve();
    }

}
