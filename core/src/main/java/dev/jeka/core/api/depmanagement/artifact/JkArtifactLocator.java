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

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Defines methods for enumerating and locating artifacts files likely to be produced.
 */
public class JkArtifactLocator {

    /**
     * Null-Case instance for {@link JkArtifactLocator}
     */
    public static final JkArtifactLocator VOID = JkArtifactLocator.of((Supplier<Path>) null, null);

    private final Supplier<Path> outputDirSupplier;

    private final Supplier<String> baseNameSupplier;

    private JkArtifactLocator(Supplier<Path> outputDirSupplier, Supplier<String> baseNameSupplier) {
        this.outputDirSupplier = outputDirSupplier;
        this.baseNameSupplier = baseNameSupplier;
    }

    /**
     * Creates a new JkArtifactLocator with the provided output directory supplier and base name supplier.
     *
     * @param outputDirSupplier A supplier that provides the output directory path.
     * @param baseNameSupplier  A supplier that provides the base name.
     */
    public static JkArtifactLocator of(Supplier<Path> outputDirSupplier,
                                        Supplier<String> baseNameSupplier) {
        return new JkArtifactLocator(outputDirSupplier, baseNameSupplier);
    }

    /**
     * Creates a new JkArtifactLocator with the provided output directory and artifact base name.
     *
     * @param outputDir The output directory path.
     * @param artifactBaseName The base name of the artifact.
     */
    public static JkArtifactLocator of(Path outputDir, String artifactBaseName) {
        return of(() -> outputDir, () -> artifactBaseName);
    }

    /**
     * Returns file system path where is supposed to be produced the specified artifact file id. This method is supposed
     * to only returns the file reference and not generate it.
     */
    public Path getArtifactPath(JkArtifactId artifactId) {
        JkUtilsAssert.state(outputDirSupplier != null,
                "Cannot find artifact %s cause the artifact locator has not been configured to locate extra artifacts.",
                artifactId);
        return outputDirSupplier.get().resolve(artifactId.toFileName(baseNameSupplier.get()));
    }

    /**
     * Returns the path where the specified artifact file is supposed to be produced. This method is intended
     * to only return the file reference and not generate it.
     */
    public Path getArtifactPath(String qualifier, String extension) {
        return getArtifactPath(JkArtifactId.of(qualifier, extension));
    }

    /**
     * Returns the main artifact file id for this producer. By default, it returns an artifact file id with no
     * classifier and 'jar' getExtension.
     */
    public JkArtifactId getMainArtifactId() {
        return JkArtifactId.of(JkArtifactId.MAIN_ARTIFACT_CLASSIFIER, getMainArtifactExt());
    }

    /**
     * Returns the extension used by the main artifact.
     */
    public String getMainArtifactExt() {
        return "jar";
    }

    /**
     * Returns the main artifact path.
     */
    public Path getMainArtifactPath() {
        return getArtifactPath(getMainArtifactId());
    }

    @Override
    public String toString() {
        return "mainArtifactPath=" + getMainArtifactPath();
    }
}
