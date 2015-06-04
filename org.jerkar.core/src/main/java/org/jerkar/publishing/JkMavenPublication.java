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
		return new JkMavenPublication(name, file, Collections.EMPTY_MAP, null, Collections.EMPTY_SET, null, null);
	}

	private final Map<String, File> artifacts;

	private final Set<String> extraExtensions; // can contains signature or checksum

	private final String artifactName;

	private final File mainArtifact;

	private final JkMavenPublicationInfo extraInfo;

	private final File secretRing;

	private final String secretRingPassword;

	private JkMavenPublication(String artifactName, File mainArtifact,
			Map<String, File> artifacts, JkMavenPublicationInfo extraInfo, Set<String> extraExtensions,
			File secretRing, String secretRingPasswsord) {
		super();
		this.artifactName = artifactName;
		this.mainArtifact = mainArtifact;
		this.artifacts = artifacts;
		this.extraInfo = extraInfo;
		this.extraExtensions = extraExtensions;
		this.secretRing = secretRing;
		this.secretRingPassword = secretRingPasswsord;
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
		return new JkMavenPublication(this.artifactName, mainArtifact, map, this.extraInfo, this.extraExtensions, null, null);
	}

	/**
	 * Returns a new publication based on this one but with the specified publication extra info required
	 * to publish on Maven central repository.
	 */
	public JkMavenPublication with(JkMavenPublicationInfo extraInfo) {
		return new JkMavenPublication(this.artifactName, this.mainArtifact, this.artifacts, extraInfo, this.extraExtensions, this.secretRing, this.secretRingPassword);
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

	/**
	 * By adding a suffix, you will deploy also all files where name is
	 * one of the published files + one of the specified suffix.
	 */
	public JkMavenPublication andAllFilesSuffixedWith(String ...suffix) {
		final Set<String> set = new HashSet<String>(extraExtensions);
		set.addAll(Arrays.asList(suffix));
		return new JkMavenPublication(artifactName, mainArtifact, artifacts, extraInfo, set, secretRing, secretRingPassword);
	}

	/**
	 * Same as #andAllFilesSuffixedWith(String...) but only apply if the specified conditional is <code>true</code>.
	 * @see #andAllFilesSuffixedWith(String...)
	 */
	public JkMavenPublication andAllFilesSuffixedWithIf(boolean condition, String ...suffix) {
		if (condition) {
			return andAllFilesSuffixedWith(suffix);
		}
		return this;
	}

	/**
	 * Returns an identical JkMavenPublication to this one but without any suffixed files.
	 * @see #andAllFilesSuffixedWith(String...)
	 * 
	 */
	@SuppressWarnings("unchecked")
	public JkMavenPublication withNoFilesSuffixed() {
		return new JkMavenPublication(artifactName, mainArtifact, artifacts, extraInfo,
				Collections.EMPTY_SET, secretRing, secretRingPassword);
	}

	/**
	 * Returns an identical JkMavenPublication to this one but holding the specified PGP information./
	 * This PGP information will be used to sign the pom file to be created.
	 */
	public JkMavenPublication withPomPgpSignatureIf(boolean conditional, File secretRing, String secretKeyPassword) {
		if (conditional) {
			return new JkMavenPublication(artifactName, mainArtifact, artifacts, extraInfo,
					extraExtensions, secretRing, secretKeyPassword);
		}
		return this;
	}

	public File mainArtifactFile() {
		return this.mainArtifact;
	}

	public Map<String, File> extraArtifacts() {
		return Collections.unmodifiableMap(this.artifacts);
	}

	public Set<File> extraFiles(String classifier) {
		final Set<File> result = new HashSet<File>();
		for (final String extension : this.extraExtensions) {
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

	public File secretRing() {
		return this.secretRing;
	}

	public String secretRingPassword() {
		return this.secretRingPassword;
	}

	@Override
	public String toString() {
		return artifacts.toString();
	}





}
