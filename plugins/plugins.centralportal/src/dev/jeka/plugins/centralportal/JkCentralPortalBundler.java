/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.plugins.centralportal;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;

import java.nio.file.Path;

/**
 * Handles the bundling process for deploying Maven publications onto a central portal.
 * This class provides functionality to sign and bundle Maven artifacts into a specified
 * zip path, using a temporary local repository.
 */
public class JkCentralPortalBundler {

    private final Path localTempRepo;

    private final JkFileSigner signer;

    private JkCentralPortalBundler(Path localTempRepo, JkFileSigner signer) {
        this.localTempRepo = localTempRepo;
        this.signer = signer;
    }

    /**
     * Creates a new instance of {@code JkCentralPortalBundler} with the specified local
     * temporary repository path and file signer.
     *
     * @param localTempRepo the path to the local temporary repository where the files
     *                      to be bundled will be stored
     * @param signer the {@code JkFileSigner} used to sign the files to be bundled
     * @return a new instance of {@code JkCentralPortalBundler} initialized with the provided
     *         local repository path and file signer
     */
    public static JkCentralPortalBundler of(Path localTempRepo, JkFileSigner signer) {
        return new JkCentralPortalBundler(localTempRepo, signer);
    }

    /**
     * Bundles a specified Maven publication by signing it, storing it in a temporary local
     * repository, and creating a zip archive at the specified path.
     *
     * @param mavenPublication the Maven publication to be bundled
     * @param bundlePath the path where the zip bundle should be created
     */
    public void bundle(JkMavenPublication mavenPublication, Path bundlePath) {
        JkMavenPublication publication = mavenPublication.copy();
        publication.setRepos(centralPortalLocalRepo().toSet());
        publication.setDefaultSigner(signer);
        publication.publish();
        JkModuleId moduleId = mavenPublication.getModuleId();
        String group = moduleId.getGroup();
        String name = moduleId.getName();
        String pathString = group.replace('.', '/') + "/" + name + "/" + publication.getVersion();
        JkPathTree.of(localTempRepo).andMatching(pathString + "/*").zipTo(bundlePath);
    }

    private JkRepo centralPortalLocalRepo() {
        JkRepo repo =  JkRepo.of(this.localTempRepo);
        repo.publishConfig.setChecksumAlgos("sha1", "md5");
        repo.publishConfig.setSignatureRequired(true);
        repo.publishConfig.setSigner(signer);
        repo.publishConfig.setVersionFilter(version -> !version.isSnapshot());
        return repo;
    }

}
