/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.depmanagement.artifact;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Objects;

/**
 * Artifacts are files produced by projects in order to be published and reused by other projects. {@link JkArtifactId}
 * identifies artifacts within a project. <p>
 * The identifier is compound of a classifier and a file extension. The classifier maps with the Maven 'classifier' concept.<br/>
 * In a project, we distinguish the <i>main artifact</i> from the others : the main artifact classifier values to empty string.
 */
public final class JkArtifactId {

    public static final String MAIN_ARTIFACT_CLASSIFIER = "";

    public static final JkArtifactId MAIN_JAR_ARTIFACT_ID = of(MAIN_ARTIFACT_CLASSIFIER, "jar");

    public static final JkArtifactId SOURCES_ARTIFACT_ID = of("sources", "jar");
    public static final JkArtifactId JAVADOC_ARTIFACT_ID = of("javadoc", "jar");

    private final String classifier;

    private final String extension;

    private JkArtifactId(String classifier, String extension) {
        this.classifier = classifier.toLowerCase();
        this.extension = extension == null || extension.trim().length() == 0 ? null : extension.trim().toLowerCase();
    }

    /**
     * Creates an artifact id with the specified classifier and extension. <p>
     * The classifier cannot be null or be a string composed of spaces.
     * An empty string extension generally means that the file has no extension.<br/>
     */
    public static JkArtifactId of(String classifier, String extension) {
        JkUtilsAssert.argument(classifier != null, "Artifact classifier cannot be null");
        JkUtilsAssert.argument(extension != null, "Artifact extension cannot be null (but blank is ok).");
        JkUtilsAssert.argument(MAIN_ARTIFACT_CLASSIFIER.equals(classifier) || !JkUtilsString.isBlank(classifier),
                "Artifact classifier cannot be a blank string.");
        return new JkArtifactId(classifier, extension);
    }

    /**
     * Shorthand for <code>of(MAIN_ARTIFACT_NAME, String)</code>.
     */
    public static JkArtifactId ofMainArtifact(String extension) {
        return JkArtifactId.of(MAIN_ARTIFACT_CLASSIFIER, extension);
    }

    public boolean isMainArtifact() {
        return MAIN_ARTIFACT_CLASSIFIER.equals(this.classifier);
    }

    /**
     * Returns the classifier of this object.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Returns the file getExtension of this object.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns the filename of this artifactId, specifying the base filename for computing
     * the final name.
     * @param namePart The base name for computing the complete filename of the artifact.
     *                 This is generally the name (or moduleId) of the project generating this artifact.
     */
    public String toFileName(String namePart) {
        String classifier = isMainArtifact() ? "" : "-" + getClassifier();
        String ext = JkUtilsString.isBlank(extension) ? "" : "." + getExtension();
        return namePart + classifier + ext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkArtifactId that = (JkArtifactId) o;

        if (!Objects.equals(classifier, that.classifier)) return false;
        return Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        int result = classifier != null ? classifier.hashCode() : 0;
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String classif = MAIN_ARTIFACT_CLASSIFIER.equals(classifier) ? "[main]" : "-" + classifier;
        return classif + '.' + extension;
    }

    /**
     * Returns a representation of the artifact id in the format "[classifier, extension]".
     */
    public String toSquareNotation() {
        return "[" + classifier + ", " + extension + "]";
    }

}
