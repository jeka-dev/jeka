package org.jake.publishing;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jake.utils.JakeUtilsAssert;

public final class JakeMavenPublication {

	@SuppressWarnings("unchecked")
	public static JakeMavenPublication of(String name, File file) {
		return new JakeMavenPublication(name, file, Collections.EMPTY_MAP);
	}

	private final Map<String, File> artifacts;

	private final String artifactName;

	private final File mainArtifact;

	private JakeMavenPublication(String artifactName, File mainArtifact, Map<String, File> artifacts) {
		super();
		this.artifactName = artifactName;
		this.mainArtifact = mainArtifact;
		this.artifacts = artifacts;
	}

	public JakeMavenPublication andIf(boolean condition, File file, String classifier) {
		if (condition) {
			return and(file, classifier);
		}
		return this;
	}

	public JakeMavenPublication and(File file, String classifier) {
		JakeUtilsAssert.isTrue(!classifier.isEmpty(), "classifier cannot be empty");
		if (artifacts.containsKey(classifier)) {
			throw new IllegalArgumentException("Can't add artifact with extension/classifier equals to ["
					+ classifier + "] as this combination is yet present in this publication " + this);
		}
		final Map<String, File> map = new HashMap<String, File>(artifacts);
		map.put(classifier, file);
		return new JakeMavenPublication(this.artifactName, mainArtifact, map);
	}

	public JakeMavenPublication andOptional(File file, String classifier) {
		if (file.exists()) {
			return and(file, classifier);
		}
		return this;
	}

	public JakeMavenPublication andOptionalIf(boolean conditional, File file, String classifier) {
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
