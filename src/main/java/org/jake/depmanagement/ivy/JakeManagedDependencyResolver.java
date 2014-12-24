package org.jake.depmanagement.ivy;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependencyResolver;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeVersionedModule;

public class JakeManagedDependencyResolver extends JakeDependencyResolver {


	private final JakeIvy jakeIvy;

	private final JakeDependencies dependencies;

	private final JakeResolutionParameters parameters;

	// Not necessary but nice if present in order to let Ivy hide data efficiently.
	private final JakeVersionedModule module;

	public JakeManagedDependencyResolver(JakeIvy jakeIvy, JakeDependencies dependencies, JakeResolutionParameters parameters, JakeVersionedModule module) {
		this.jakeIvy = jakeIvy;
		this.dependencies = dependencies;
		this.parameters = parameters;
		this.module = module;
	}

	@Override
	protected List<File> getDeclaredDependencies(JakeScope scope) {
		final Set<JakeArtifact> artefacts;
		if (module != null) {
			artefacts = jakeIvy.resolve(module, dependencies, scope, parameters);
		} else {
			artefacts = jakeIvy.resolve(dependencies, scope, parameters);
		}
		final List<File> result = new LinkedList<File>();
		for (final JakeArtifact artifact : artefacts) {
			result.add(artifact.localFile());
		}
		return result;
	}

	@Override
	public Set<JakeScope> declaredScopes() {
		return this.dependencies.moduleScopes();
	}

}
