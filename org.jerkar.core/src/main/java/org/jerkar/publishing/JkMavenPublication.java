package org.jerkar.publishing;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.utils.JkUtilsAssert;

public final class JkMavenPublication {

	@SuppressWarnings("unchecked")
	public static JkMavenPublication of(String name, File file) {
		return new JkMavenPublication(name, file, Collections.EMPTY_MAP);
	}

	private final Map<String, File> artifacts;

	private final String artifactName;

	private final File mainArtifact;

	private JkMavenPublication(String artifactName, File mainArtifact, Map<String, File> artifacts) {
		super();
		this.artifactName = artifactName;
		this.mainArtifact = mainArtifact;
		this.artifacts = artifacts;
	}

	public JkMavenPublication andIf(boolean condition, File file, String classifier) {
		if (condition) {
			return and(file, classifier);
		}
		return this;
	}

	public JkMavenPublication and(File file, String classifier) {
		JkUtilsAssert.isTrue(!classifier.isEmpty(), "classifier cannot be empty");
		if (artifacts.containsKey(classifier)) {
			throw new IllegalArgumentException("Can't add artifact with extension/classifier equals to ["
					+ classifier + "] as this combination is yet present in this publication " + this);
		}
		final Map<String, File> map = new HashMap<String, File>(artifacts);
		map.put(classifier, file);
		return new JkMavenPublication(this.artifactName, mainArtifact, map);
	}

	public JkMavenPublication andOptional(File file, String classifier) {
		if (file.exists()) {
			return and(file, classifier);
		}
		return this;
	}

	public JkMavenPublication andOptionalIf(boolean conditional, File file, String classifier) {
		if (conditional) {
			return andOptional(file, classifier);
		}
		return this;
	}

	public File mainArtifactFile() {
		return this.mainArtifact;
	}

	public Map<String, File> extraArtifacts() {
		return Collections.unmodifiableMap(this.artifacts);
	}

	public String artifactName() {
		return artifactName;
	}



	@Override
	public String toString() {
		return artifacts.toString();
	}



}
