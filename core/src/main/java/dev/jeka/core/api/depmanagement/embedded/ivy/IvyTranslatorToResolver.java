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

package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.plugins.resolver.*;
import org.apache.ivy.util.url.CredentialsStore;

import java.io.File;
import java.net.URL;

class IvyTranslatorToResolver {

    private static final String MAVEN_ARTIFACT_PATTERN =
            "/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

    static AbstractResolver toChainResolver(JkRepoSet repos) {
        final ChainResolver chainResolver = new ChainResolver();
        for (final JkRepo jkRepo : repos.getRepos()) {
            final DependencyResolver resolver = toResolver(jkRepo, true);
            resolver.setName(jkRepo.toString());
            chainResolver.add(resolver);
        }
        return chainResolver;
    }

    static RepositoryResolver convertToPublishAndBind(JkRepo repo, IvySettings ivySettings) {
        RepositoryResolver resolver = toResolver(repo, false);
        ivySettings.addResolver(resolver);
        return resolver;
    }

    // see
    // http://www.draconianoverlord.com/2010/07/18/publishing-to-maven-repos-with-ivy.html
    private static RepositoryResolver toResolver(JkRepo repo, boolean download) {
        if (!repo.isIvyRepo()) {
            if (!isFileSystem(repo.getUrl()) || download) {
                return ibiblioResolver(repo);
            }
            return mavenFileSystemResolver(repo);
        }
        final JkRepo.JkRepoIvyConfig ivyRepoConfig = repo.getIvyConfig();
        if (isFileSystem(repo.getUrl())) {
            final FileRepository fileRepo = new FileRepository(new File(repo.getUrl().getPath()));
            final FileSystemResolver result = new FileSystemResolver();
            result.setRepository(fileRepo);
            for (final String pattern : ivyRepoConfig.artifactPatterns()) {
                result.addArtifactPattern(completePattern(repo.getUrl().getPath(), pattern));
            }
            for (final String pattern : ivyRepoConfig.ivyPatterns()) {
                result.addIvyPattern(completePattern(repo.getUrl().getPath(), pattern));
            }
            return result;
        }
        if (repo.getUrl().getProtocol().equals("http") || repo.getUrl().getProtocol().equals("https")) {
            final IvyRepResolver result = new IvyRepResolver();
            result.setIvyroot(repo.getUrl().toString());
            result.setArtroot(repo.getUrl().toString());
            result.setArtpattern(IvyRepResolver.DEFAULT_IVYPATTERN);
            result.setIvypattern(IvyRepResolver.DEFAULT_IVYPATTERN);
            result.setM2compatible(true);
            if (isHttp(repo.getUrl())) {
                final JkRepo.JkRepoCredentials credentials = repo.getCredentials();
                if (!CredentialsStore.INSTANCE.hasCredentials(repo.getUrl().getHost()) && credentials != null) {
                    CredentialsStore.INSTANCE.addCredentials(credentials.getRealm(), repo.getUrl().getHost(),
                            credentials.getUserName(), credentials.getPassword());

                }
            }
            result.setChangingPattern("\\*-SNAPSHOT");
            result.setCheckmodified(true);
            return result;
        }
        throw new IllegalStateException(repo.getUrl() .getProtocol()+ " not handled for translating repo "+ repo);
    }

    private static boolean isFileSystem(URL url) {
        return url.getProtocol().equals("file");
    }

    private static boolean isHttp(URL url) {
        return url.getProtocol().equals("http") || url.getProtocol().equals("https");
    }

    private static IBiblioResolver ibiblioResolver(JkRepo repo) {
        final IBiblioResolver result = new IBiblioResolver();
        result.setM2compatible(true);
        result.setUseMavenMetadata(true);
        result.setRoot(repo.getUrl().toString());
        result.setUsepoms(true);
        if (isHttp(repo.getUrl())) {
            final JkRepo.JkRepoCredentials credentials = repo.getCredentials();
            if (!CredentialsStore.INSTANCE.hasCredentials(repo.getUrl().getHost()) && credentials != null) {
                CredentialsStore.INSTANCE.addCredentials(credentials.getRealm(),
                        repo.getUrl().getHost(), credentials.getUserName(), credentials.getPassword());

            }
        }
        result.setChangingPattern("\\*-SNAPSHOT");
        result.setCheckmodified(true);
        return result;
    }

    private static FileSystemResolver mavenFileSystemResolver(JkRepo repo) {
        final FileRepository fileRepo = new FileRepository(new File(repo.getUrl().getPath()));
        final FileSystemResolver result = new FileSystemResolver();
        result.setRepository(fileRepo);
        result.addArtifactPattern(completePattern(repo.getUrl().getPath(), MAVEN_ARTIFACT_PATTERN));
        result.setM2compatible(true);
        return result;
    }

    private static String completePattern(String url, String pattern) {
        return url + "/" + pattern;
    }

}
