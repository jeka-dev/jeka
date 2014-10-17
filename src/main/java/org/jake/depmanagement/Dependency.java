package org.jake.depmanagement;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.depmanagement.JakeScope.JakeScopeMapping;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;


public abstract class Dependency {

	public static ModuleAndVersionRange of(JakeModuleId module, JakeVersionRange version, JakeScopeMapping mapping) {
		return new ModuleAndVersionRange(module, version, mapping);
	}

	public static ModuleAndVersionRange of(JakeModuleId module, JakeVersionRange version) {
		return of(module, version, JakeScopeMapping.oneToOne());
	}

	public static ModuleAndVersionRange of(String organisation, String name, String version) {
		return of(JakeModuleId.of(organisation, name), JakeVersionRange.of(version), JakeScopeMapping.oneToOne());
	}

	public static ModuleAndVersionRange.ModuleOnly of(JakeModuleId module) {
		final ModuleAndVersionRange moduleAndVersion = of(module, null);
		return new ModuleAndVersionRange.ModuleOnly(moduleAndVersion);
	}

	public static ModuleAndVersionRange.ModuleOnly of(String organisation, String name) {
		return of(JakeModuleId.of(organisation, name));
	}

	public static ModuleAndVersionRange of(String groupAndNameAndVersion) {
		final String[] strings = JakeUtilsString.split(groupAndNameAndVersion, ":");
		if (strings.length != 3) {
			throw new IllegalArgumentException("Module should be formated as 'groupName:moduleName:version'. Was " + groupAndNameAndVersion);
		}
		return of(strings[0], strings[1], strings[2]);
	}

	public static class ModuleAndVersionRange extends Dependency {

		private final JakeModuleId module;
		private final JakeVersionRange versionRange;

		public ModuleAndVersionRange(JakeModuleId module, JakeVersionRange versionRange, JakeScopeMapping mapping) {
			super(mapping);
			this.module = module;
			this.versionRange = versionRange;
		}

		public ModuleAndVersionRange.AfterScope scope(JakeScope...scopes) {
			final List<JakeScopeMapping> list = new LinkedList<JakeScope.JakeScopeMapping>();
			for (final JakeScope scope : scopes) {
				list.add(scope.mapToDefault());
			}
			final JakeScopeMapping mapping = this.scopeMapping().and(list);
			return new AfterScope(module, versionRange, mapping, list);
		}

		public JakeModuleId module() {
			return module;
		}

		public JakeVersionRange versionRange() {
			return versionRange;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "=" + module + ":" + versionRange;
		}



		public static final class ModuleOnly {
			private final ModuleAndVersionRange module;

			public ModuleOnly(ModuleAndVersionRange module) {
				super();
				this.module = module;
			}

			public ModuleAndVersionRange rev(JakeVersionRange version) {
				return new ModuleAndVersionRange(module.module, version, module.scopeMapping());
			}

			public ModuleAndVersionRange rev(String version) {
				return rev(JakeVersionRange.of(version));
			}
		}

		public static final class AfterScope extends ModuleAndVersionRange {

			private final List<JakeScopeMapping> contextMappings;

			private AfterScope(JakeModuleId module,
					JakeVersionRange versionRange, JakeScopeMapping mapping, List<JakeScopeMapping> context) {
				super(module, versionRange, mapping);
				this.contextMappings = context;
			}

			public ModuleAndVersionRange mapTo(JakeScope ... scopes) {
				return new ModuleAndVersionRange(module(), versionRange(), scopeMapping().minus(contextMappings));
			}

		}


	}

	public static final class Files extends Dependency {

		private final Iterable<File> files;

		private Files(Iterable<File> files, JakeScopeMapping mapping) {
			super(mapping);
			this.files = JakeUtilsIterable.toList(files);
		}

	}

	private final JakeScopeMapping scopeMapping;

	private Dependency(JakeScopeMapping scopeMapping) {
		this.scopeMapping = scopeMapping;
	}

	public JakeScopeMapping scopeMapping() {
		return scopeMapping;
	}

}
