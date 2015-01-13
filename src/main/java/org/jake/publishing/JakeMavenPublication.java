package org.jake.publishing;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

public class JakeMavenPublication implements Iterable<JakeMavenPublication.Artifact> {

	public static JakeMavenPublication of(File file) {
		final Artifact artifact = new Artifact(file, null);
		final ArtifactKey key = new ArtifactKey(artifact);
		return new JakeMavenPublication(JakeUtilsIterable.mapOf(key, artifact));
	}

	private final Map<ArtifactKey, Artifact> artifacts;

	private JakeMavenPublication(Map<ArtifactKey, Artifact> artifacts) {
		super();
		this.artifacts = artifacts;
	}

	public JakeMavenPublication and(File file, String classifier) {
		final Artifact artifact = new Artifact(file, classifier);
		final ArtifactKey key = new ArtifactKey(artifact);
		if (artifacts.containsKey(key)) {
			throw new IllegalArgumentException("Can't add artifact with extension/classifier equals to ["
					+ key + "] as this combination is yet present in this publication " + this);
		}
		final Map<ArtifactKey, Artifact> map = new HashMap<ArtifactKey, Artifact>(artifacts);
		map.put(key, artifact);
		return new JakeMavenPublication(map);
	}

	@Override
	public Iterator<Artifact> iterator() {
		return artifacts.values().iterator();
	}

	@Override
	public String toString() {
		return artifacts.toString();
	}

	public static class Artifact {

		private Artifact(File file, String classifier) {
			super();
			if (!file.getName().contains(".")) {
				throw new IllegalArgumentException("File " + file + " does not have an extension. Extension is necessary to determine the type of the artifact." );
			}
			this.file = file;
			this.classifier = classifier;
		}

		private final File file;

		private final String classifier;

		public String name() {
			return JakeUtilsString.substringBeforeLast(file.getName(), ".");
		}

		public String extension() {
			return JakeUtilsString.substringAfterLast(file.getName(), ".");
		}

		public File file() {
			return file;
		}

		public String classifier() {
			return  classifier;
		}

		@Override
		public String toString() {
			return file.toString() + classifier == null ? "" : ":" + classifier;
		}

	}


	private static class ArtifactKey {
		private final String name;
		private final String extension;

		public ArtifactKey(Artifact artifact) {
			super();
			this.name = artifact.name();
			this.extension = artifact.extension();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((extension == null) ? 0 : extension.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			final ArtifactKey other = (ArtifactKey) obj;
			if (extension == null) {
				if (other.extension != null) {
					return false;
				}
			} else if (!extension.equals(other.extension)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return this.name + "," + extension;
		}



	}


}
