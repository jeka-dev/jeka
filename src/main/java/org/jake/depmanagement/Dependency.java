package org.jake.depmanagement;

import java.io.File;
import java.util.List;

public abstract class Dependency {

	public abstract List<File> getArtifacts();

	public ModuleAndVersion of(Module module, VersionRange version) {
		return new ModuleAndVersion(module, version);
	}

	public static class ModuleAndVersion {
		private final Module module;
		private final VersionRange versionRange;

		public ModuleAndVersion(Module module, VersionRange versionRange) {
			this.module = module;
			this.versionRange = versionRange;
		}
	}



	public static abstract class VersionRange {

	}

}
