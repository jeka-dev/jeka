package org.jake.depmanagement;

import java.io.File;

public final class JakeArtifact {

	public static JakeArtifact of(JakeVersionedModule versionedModule, File localFile) {
		return new JakeArtifact(versionedModule, localFile);
	}

	private final JakeVersionedModule versionedModule;

	private final File localFile;

	private JakeArtifact(JakeVersionedModule versionedModule, File localFile) {
		super();
		this.versionedModule = versionedModule;
		this.localFile = localFile;
	}

	public JakeVersionedModule versionedModule() {
		return versionedModule;
	}

	public File localFile() {
		return localFile;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + localFile.hashCode();
		result = prime * result + versionedModule.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final JakeArtifact other = (JakeArtifact) obj;
		if (!localFile.equals(other.localFile)) {
			return false;
		}
		if (!versionedModule.equals(other.versionedModule)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "JakeArtifact [versionedModule=" + versionedModule
				+ ", localFile=" + localFile + "]";
	}



}
