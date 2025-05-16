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

package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Provides static methods for creating repositories from standard properties.
 * <p>
 * Basic configuration can be achieved by setting below options.
 * <pre>
 * jeka.repos.download= (url)
 * jeka.repos.download.username=    (optional)
 * jeka.repos.download.password=    (optional)
 * jeka.repos.download.realm=    (optional)
 * jeka.repos.download.headers.myHeaderName= (optional : any header name fits)
 * jeka.repos.download.headers.anotherHeaderName= (optional any header name fits)
 *
 * jeka.repos.publish=  (url)
 * jeka.repos.publish.username=     (optional)
 * jeka.repos.publish.password=     (optional)
 * jeka.repos.publish.realm=     (optional)
 * jeka.repos.publish.headers.myHeaderName= (optional : any header name fits)
 * </pre>
 *
 * <p>
 *  If you deal with many repositories, you can override basic setting using named repositories. <br/>
 * <pre>
 * jeka.repos.aRepoName= (url)
 * jeka.repos.aRepoName.username=    (optional)
 * jeka.repos.aRepoName.password=    (optional)
 *
 * jeka.repos.anotherRepoName= (url)
 * jeka.repos.anotherRepoName.username=    (optional)
 * jeka.repos.anotherRepoName.password=    (optional)
 * jeka.repos.anotherRepoName.headers.myHeaderName= (optional : any header name fits)
 *
 * jeka.repos.download.name=aRepoName, anotherRepoName
 *
 * </pre>
 * If  <i>download</i> or <i>publish</i> repo is defined to use a named repo (as jeka.repos.download.name=aRepoName),
 * this takes precedence over basic configuration.
 */
public class JkRepoProperties {

    public static final String MAVEN_CENTRAL_ALIAS = "mavenCentral";

    public static final String JEKA_GITHUB_ALIAS = "jekaGithub";

    private final JkProperties properties;

    private JkRepoProperties(JkProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a new JkRepoProperties object from the specified JkProperties.
     */
    public static JkRepoProperties of(JkProperties properties) {
        return new JkRepoProperties(properties);
    }

    /**
     * Creates a new JkRepoProperties based on global configuration.
     */
    public static JkRepoProperties ofGlobalProperties() {
        return of(JkProperties.ofStandardProperties());
    }

    /**
     * Returns repository where are published artifacts. Returns <code>null</code> if no download publish repo is defined.
     */
    public JkRepoSet getPublishRepository() {
        return getRepos("jeka.repos.publish");
    }

    /**
     * Returns repo from where are downloaded dependencies. Returns Maven central repo if no download repository is defined.
     */
    public JkRepoSet getDownloadRepos() {
        JkRepoSet repoSet = getRepos("jeka.repos.download");
        if (repoSet.getRepos().isEmpty()) {
            repoSet = repoSet.and(JkRepo.ofMavenCentral());
        }
        return repoSet;
    }

    /**
     * Creates {@link JkRepo} form Jeka options. the specified repository name
     * will be turned to <code>repo.[repoName].url</code>,
     * <code>repo.[repoName].username</code> and
     * <code>repo.[repoName].password</code> options for creating according
     * repository.
     */
    public JkRepo getRepoByName(String name) {
        if (MAVEN_CENTRAL_ALIAS.equals(name)) {
            return JkRepo.ofMavenCentral();
        }
        String property = "jeka.repos." + name;
        String url = properties.get(property);
        JkRepo result;
        if (JEKA_GITHUB_ALIAS.equals(name)) {
            result = JkRepo.ofGitHub("jeka-dev", "jeka");
        } else if (JkUtilsString.isBlank(url)) {
            return null;
        } else {
            result = JkRepo.of(url);
        }
        JkRepo.JkRepoCredentials credentials = getCredentials(property);
        result.mergeCredential(credentials);
        return result;
    }

    public String getPublishUsername() {
        return properties.get("jeka.repos.publish.username");
    }

    public String getPublishPassword() {
        return properties.get("jeka.repos.publish.password");
    }

    private JkRepoSet getRepos(String propertyName) {
        String nameOrUrls = properties.get(propertyName);
        if (JkUtilsString.isBlank(nameOrUrls)) {
            return JkRepoSet.of();
        }
        List<JkRepo> repos = Arrays.stream(nameOrUrls.split(","))
                .map(String::trim)
                .map(nameOrUrl -> getRepo(propertyName, nameOrUrl))
                .collect(Collectors.toList());
        return JkRepoSet.of(repos);
    }

    private JkRepo getRepo(String repoNameProperty, String nameOrUrl) {
        if (isUrl(nameOrUrl)) {
            Map<String, String> headers = new HashMap<>(getHttpHeaders(repoNameProperty));
            JkRepo repo = JkRepo.of(nameOrUrl);
            JkRepo.JkRepoCredentials credentials = getCredentials(repoNameProperty);
            if (!repo.hasAuthorizationHeader() && !credentials.isEmpty()) {
                String encoded64 = credentials.encodedBase64();
                headers.put("Authorization", encoded64);
            }
            repo.setCredentials(credentials);
            repo.setHttpHeaders(headers);
            return repo;
        }
        return getRepoByName(nameOrUrl);
    }

    private JkRepo.JkRepoCredentials getCredentials(String prefix) {
        String userName = properties.get(prefix + ".username");
        String password = properties.get(prefix + ".password");
        String realm = properties.get(prefix + ".realm");
        return JkRepo.JkRepoCredentials.of(userName, password, realm);
    }

    private Map<String, String> getHttpHeaders(String prefix) {
        String headersPropName = prefix + ".headers.";
        return properties.getAllStartingWith(headersPropName, false);
    }

    private static List<String> urlOrNames(String value) {
        return Arrays.stream(value.trim().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static boolean isUrl(String nameOrUrl) {
        return JkUtilsString.startsWithAny(nameOrUrl, "http:", "https:", "file:");
    }

}