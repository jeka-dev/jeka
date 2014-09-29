package org.jake.ivy;

import java.io.File;
import java.io.IOException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;

public class JakeIvy {

	public void retrieve() {
		final DependencyResolver dependencyResolver = new IBiblioResolver();

		final IvySettings ivySettings = new IvySettings();
		ivySettings.addResolver(dependencyResolver);

		final Ivy ivy = Ivy.newInstance(ivySettings);

		final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(
				"mygroupId",
				"amyrtifactId-envelope",
				"myversion"
				);

		final ModuleRevisionId dependee = ModuleRevisionId.newInstance("org.apache.tomcat", "tomcat-catalina", "8.0.12");

		// 1st create an ivy module (this always(!) has a "default" configuration already)
		final DefaultModuleDescriptor moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(thisModuleRevisionId);

		// don't go transitive here, if you want the single artifact
		final DefaultDependencyDescriptor dependencyDescriptor = new DefaultDependencyDescriptor(moduleDescriptor, dependee, false, false, false);

		// map to master to just get the code jar. See generated ivy module xmls from maven repo
		// on how configurations are mapped into ivy. Or check
		// e.g. http://lightguard-jp.blogspot.de/2009/04/ivy-configurations-when-pulling-from.html
		dependencyDescriptor.addDependencyConfiguration("default", "master");
		moduleDescriptor.addDependency(dependencyDescriptor);

		// now resolve
		final ResolveOptions resolveOptions = new ResolveOptions().setConfs(new String[]{"default"});
		ResolveReport reportResolver;
		try {
			reportResolver = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (final Exception e1) {
			throw new RuntimeException(e1);
		}
		if (reportResolver.hasError()) {
			System.out.println("*************************************************************************");
			System.out.println(reportResolver);

			throw new RuntimeException(reportResolver.getAllProblemMessages().toString());
		}

		final String filePattern = new File("out").getAbsolutePath() +"/[artifact](-[classifier]).[ext]";
		final RetrieveOptions retrieveOptions = new RetrieveOptions().setConfs(new String[]{"default"});
		try {
			ivy.retrieve(moduleDescriptor.getModuleRevisionId(), filePattern, retrieveOptions);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}



	}

}
