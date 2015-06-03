package org.jerkar.publishing;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jerkar.utils.JkUtilsAssert;

public final class JkMavenPublication {

	@SuppressWarnings("unchecked")
	public static JkMavenPublication of(String name, File file) {
		return new JkMavenPublication(name, file, Collections.EMPTY_MAP, null, Collections.EMPTY_SET);
	}

	private final Map<String, File> artifacts;

	private final Set<String> extraExtension; // can contains signature or checksum

	private final String artifactName;

	private final File mainArtifact;

	private final JkMavenPublicationInfo extraInfo;

	private JkMavenPublication(String artifactName, File mainArtifact, Map<String, File> artifacts, JkMavenPublicationInfo extraInfo, Set<String> extraFiles) {
		super();
		this.artifactName = artifactName;
		this.mainArtifact = mainArtifact;
		this.artifacts = artifacts;
		this.extraInfo = extraInfo;
		this.extraExtension = extraFiles;
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
		return new JkMavenPublication(this.artifactName, mainArtifact, map, this.extraInfo, this.extraExtension);
	}

	/**
	 * Returns a new publication based on this one but with the specified publication extra info required
	 * to publish on Maven central repository.
	 */
	public JkMavenPublication with(JkMavenPublicationInfo extraInfo) {
		return new JkMavenPublication(this.artifactName, this.mainArtifact, this.artifacts, extraInfo, extraExtension);
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

	public JkMavenPublication andAllSuffixedWith(String ...suffix) {
		final Set<String> set = new HashSet<String>(this.extraExtension);
		set.addAll(Arrays.asList(suffix));
		return new JkMavenPublication(artifactName, mainArtifact, artifacts, extraInfo, set);
	}

	public File mainArtifactFile() {
		return this.mainArtifact;
	}

	public Map<String, File> extraArtifacts() {
		return Collections.unmodifiableMap(this.artifacts);
	}

	public Set<File> extraFiles(String classifier) {
		final Set<File> result = new HashSet<File>();
		for (final String extension : this.extraExtension) {
			if (classifier == null) {
				final File file = new File(this.mainArtifact.getPath() + extension);
				if (file.exists()) {
					result.add(file);
				}
			} else {
				final File file = this.artifacts.get(classifier);
				if (file == null) {
					continue;
				}
				final File extFile = new File(file.getPath() + extension);
				if (extFile.exists()) {
					result.add(extFile);
				}
			}
		}
		return result;
	}

	public String artifactName() {
		return artifactName;
	}

	public JkMavenPublicationInfo extraInfo() {
		return this.extraInfo;
	}

	@Override
	public String toString() {
		return artifacts.toString();
	}



}
