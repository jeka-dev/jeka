package org.jake.depmanagement.ivy;

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.jake.depmanagement.JakeDependency;
import org.jake.depmanagement.JakeExternalModule;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeScopedDependency;
import org.jake.depmanagement.JakeVersionRange;

class Translators {

	public static boolean isManagedDependency(JakeDependency dependency) {
		return dependency instanceof JakeExternalModule;
	}

	/**
	 * @param scopedDependency must be of {@link JakeExternalModule}
	 */
	public static DependencyDescriptor to(JakeScopedDependency scopedDependency, JakeScopeMapping defaultMapping) {
		final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
		final DefaultDependencyDescriptor result =  new DefaultDependencyDescriptor(to(externalModule), false);
		result.a
	}

	public static ModuleRevisionId to(JakeExternalModule externalModule) {
		return new ModuleRevisionId(to(externalModule.moduleId()), to(externalModule.versionRange()));
	}

	public static ModuleId to(JakeModuleId moduleId) {
		return new ModuleId(moduleId.group(), moduleId.name());
	}

	public static String to(JakeVersionRange versionRange) {
		return versionRange.definition();
	}

}
