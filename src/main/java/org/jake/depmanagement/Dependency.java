package org.jake.depmanagement;

import java.io.File;

import org.jake.depmanagement.JakeScope.JakeScopeMapping;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

/**
 * Identifier for a dependency of a project. It can be a either : <ul>
 * <li>A module + version identifier as <code>org.hibernate:hibernate-core:3.0.+</code>,</li>
 * <li>A project inside a multi-project build,</li>
 * <li>Some files on the file system.</li>
 * </ul>
 * Each dependency is associated with a scope mapping to determine precisely in which scenario
 * the dependency is necessary.
 * 
 * @author Jerome Angibaud
 */
public abstract class Dependency {

	/**
	 * Creates a {@link ModuleAndVersionRange} dependency with the specified version and scope mapping.
	 */
	public static ModuleAndVersionRange of(JakeModuleId module, JakeVersionRange version, JakeScopeMapping mapping) {
		return new ModuleAndVersionRange(module, version, mapping);
	}

	/**
	 * Creates a {@link ModuleAndVersionRange} dependency with the specified version.
	 */
	public static ModuleAndVersionRange of(JakeModuleId module, JakeVersionRange version) {
		return of(module, version, JakeScopeMapping.empty());
	}

	/**
	 * Creates a {@link ModuleAndVersionRange} dependency with the specified version.
	 */
	public static ModuleAndVersionRange of(String organisation, String name, String version) {
		return of(JakeModuleId.of(organisation, name), JakeVersionRange.of(version), JakeScopeMapping.empty());
	}

	/**
	 * Creates a partial dependency describing the module but not the version.
	 */
	public static ModuleAndVersionRange.ModuleOnly of(JakeModuleId module) {
		final ModuleAndVersionRange moduleAndVersion = of(module, null);
		return new ModuleAndVersionRange.ModuleOnly(moduleAndVersion);
	}

	/**
	 * Creates a partial dependency describing the module but not the version.
	 */
	public static ModuleAndVersionRange.ModuleOnly of(String organisation, String name) {
		return of(JakeModuleId.of(organisation, name));
	}

	/**
	 * Creates a {@link ModuleAndVersionRange} dependency with the specified version.
	 */
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
			JakeScopeMapping mapping = this.scopeMapping();
			for (final JakeScope scope : scopes) {
				mapping = mapping.and(scope, scope);
			}
			return new AfterScope(module, versionRange, mapping, scopes.length);
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


		/**
		 * Intermediate class of the fluent API. You can't do anything from it except precise
		 * the version range.
		 */
		public static final class ModuleOnly {
			private final ModuleAndVersionRange module;

			public ModuleOnly(ModuleAndVersionRange module) {
				super();
				this.module = module;
			}

			public ModuleAndVersionRange version(JakeVersionRange version) {
				return new ModuleAndVersionRange(module.module, version, module.scopeMapping());
			}

			public ModuleAndVersionRange version(String version) {
				return version(JakeVersionRange.of(version));
			}
		}

		public static final class AfterScope extends ModuleAndVersionRange {

			private final int pending;

			private AfterScope(JakeModuleId module,
					JakeVersionRange versionRange, JakeScopeMapping mapping, int pendingCount) {
				super(module, versionRange, mapping);
				this.pending = pendingCount;
			}

			public ModuleAndVersionRange mapTo(JakeScope ... scopes) {
				final JakeScope.JakeScopeMapping mapping = scopeMapping().replaceLastEntries(pending, scopes);
				return new ModuleAndVersionRange(module(), versionRange(), mapping);
			}

		}


	}

	/**
	 * A dependency on files located on file system.
	 */
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
