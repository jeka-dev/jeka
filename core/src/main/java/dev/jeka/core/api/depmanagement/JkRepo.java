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

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.http.JkHttpResponse;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomElement;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.core.module.id.ModuleId;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Hold configuration necessary to instantiate download or upload repository
 */
public final class JkRepo {

    private static final String LOCAL_NAME = "local";

    private final static Predicate<JkVersion> SNAPSHOT_ONLY = new Predicate<JkVersion>() {

        @Override
        public boolean test(JkVersion jkVersion) {
            return jkVersion.isSnapshot();
        }

        @Override
        public String toString() {
            return "Snapshot Only";
        }
    };

    private final static Predicate<JkVersion> NO_SNAPSHOT = new Predicate<JkVersion>() {

        @Override
        public boolean test(JkVersion jkVersion) {
            return !jkVersion.isSnapshot();
        }

        @Override
        public String toString() {
            return "No Snapshot";
        }
    };

    /**
     * URL of the Maven central repository.
     */
    public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";

    /**
     * URL of the OSSRH repository for both deploying snapshot and download artifacts.
     */
    public static final String MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT = "https://oss.sonatype.org/content/repositories/snapshots/";

    /**
     * URL for the OSSRH repository for downloading released artifacts.
     */
    public static final String MAVEN_OSSRH_DOWNLOAD_RELEASE = "https://oss.sonatype.org/content/repositories/releases/";

    /**
     * URL of the OSSRH repository for deploying released artifacts.
     */
    public static final String MAVEN_OSSRH_DEPLOY_RELEASE = "https://oss.sonatype.org/service/local/staging/deploy/maven2/";

    /**
     * URL of the OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static final String MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT = "https://oss.sonatype.org/content/groups/public/";

    private static final String IVY_PREFIX = "ivy:";

    private final URL url;

    private JkRepoCredentials credentials;

    /**
     * Configuration specific to Ivy repository. Returns <code>null</code> if this configuration stands
     * for a Maven repository.
     */
    public final JkRepoIvyConfig ivyConfig;

    /**
     * Configuration specific for repository for which, we want to publish on.
     */
    public final JkPublishConfig publishConfig;

    private Map<String, String> httpHeaders = new HashMap<>();

    public final boolean ivyRepo; // true if this repository is an Ivy one, false if it is a Maven one.

    private JkRepo(URL url, boolean ivyRepo, JkRepoIvyConfig ivyConfig, JkPublishConfig publishConfig) {
        this.url = url;
        this.ivyRepo = ivyRepo;
        this.ivyConfig = ivyConfig;
        this.publishConfig = publishConfig;
    }

    private JkRepo(URL url, boolean ivyRepo) {
        this(url, ivyRepo, new JkRepoIvyConfig(), new JkPublishConfig());
    }

    /**
     * Creates a repository having the specified url. If the repository is an Ivy repository
     * than the url should start with <code>ivy:</code> as <code>ivy:http://myrepolocation</code>.
     * If specified url is "local" then it returns the local repository.
     */
    public static JkRepo of(String url) {
        if (url.equals(LOCAL_NAME)) {
            return ofLocal();
        }
        if (url.toLowerCase().startsWith(IVY_PREFIX)) {
            return new JkRepo(toUrl(url.substring(4)), true);
        }
        return new JkRepo(toUrl(url), false);
    }

    /**
     * Creates a Maven repository having the specified file location.
     */
    public static JkRepo of(Path dir) {
        dir = dir.isAbsolute() ? dir : dir.toAbsolutePath().normalize();
        return JkRepo.of(dir.toString());
    }

    /**
     * Creates the Maven central repository. If the github token is present in environment variable GITHUB_TOKEN,
     * the credential is set up using this token .
     */
    public static JkRepo ofMavenCentral() {
        return of(MAVEN_CENTRAL_URL).toReadonly();
    }

    public static JkRepo ofGitHub(String owner, String repoName) {
        String baseUrl = "https://maven.pkg.github.com/" + owner + "/" + repoName;
        String username = null;
        String pwd = null;
        String githubToken = System.getenv("GITHUB_TOKEN");
        if (!JkUtilsString.isBlank(githubToken)) {
            JkLog.verbose("Github token found, configure repo %s with associate credential", baseUrl);
            username = "GITHUB_TOKEN";
            pwd = githubToken;
        } else {
            JkLog.verbose("No Github token found to make credential on repo %s.", baseUrl);
        }
        JkRepo repo = of(baseUrl)
                .setCredentials(JkRepoCredentials.of(username, pwd, "GitHub Package Registry"));
        repo.publishConfig.setUniqueSnapshot(false);
        return repo;
    }

    /**
     * Creates an OSSRH repository for both deploying snapshot and download artifacts.
     */
    public static JkRepo ofMavenOssrhDownloadAndDeploySnapshot(String jiraId, String jiraPassword) {
        JkRepo repo = of(MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT)
                .setCredentials(JkRepoCredentials.of(jiraId, jiraPassword, "Sonatype Nexus Repository Manager"));
        repo.publishConfig
                    .setUniqueSnapshot(false)
                    .setVersionFilter(SNAPSHOT_ONLY);
        return repo;
    }

    /**
     * Creates an OSSRH repository for deploying released artifacts.
     */
    public static JkRepo ofMavenOssrhDeployRelease(String jiraId, String jiraPassword,
                                                   JkFileSigner signer) {
        JkRepo repo =  of(MAVEN_OSSRH_DEPLOY_RELEASE)
                .setCredentials(jiraId, jiraPassword, "Sonatype Nexus Repository Manager");
        repo.publishConfig
                .setSignatureRequired(true)
                .setVersionFilter(NO_SNAPSHOT)
                .setSigner(signer)
                .setChecksumAlgos("md5", "sha1");
        return repo;
    }

    /**
     * Creates a OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static JkRepo ofMavenOssrhPublicDownload() {
        return of(MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT);
    }

    /**
     * Creates a Maven repository for publishing locally under <code></code>[USER HOME]/.jeka/publish</code> folder.
     */
    public static JkRepo ofLocal() {
        final Path file = JkLocator.getJekaUserHomeDir().resolve("maven-publish-dir");
        return JkRepo.of(file);
    }

    /**
     * Creates an Ivy repository for publishing locally under <code></code>[USER HOME]/.jeka/publish</code> folder.
     */
    public static JkRepo ofLocalIvy() {
        final Path file = JkLocator.getJekaUserHomeDir().resolve("ivy-publish-dir");
        return JkRepo.of(IVY_PREFIX + file);
    }

    /**
     * Returns a new JkRepo instance with the same configuration as the current one,
     * but without any publication configuration, effectively making it read-only.
     *
     * @return a new JkRepo instance configured as read-only
     */
    public JkRepo toReadonly() {
        return new JkRepo(this.url, this.ivyRepo, this.ivyConfig, null)
                .setCredentials(this.credentials)
                .setHttpHeaders(this.httpHeaders);
    }

    /**
     * Returns if this repository is in read-only mode.
     * A repository is considered read-only if it does not have a publishing configuration.
     */
    public boolean isReadonly() {
        return this.publishConfig == null;
    }

    /**
     * Returns the url of this repository.
     */
    public URL getUrl() {
        return url;
    }


    public JkRepoIvyConfig getIvyConfig() {
        return this.ivyConfig;
    }

    public boolean isIvyRepo() {
        return this.ivyRepo;
    }

    public boolean isLocal() {
        return url.getProtocol().equals("file");
    }

    /**
     * Retrieves the credentials associated with this repository.
     *
     * @return the credentials configured for this repository, or null if no credentials are set
     */
    public JkRepoCredentials getCredentials() {
        return credentials;
    }

    /**
     * Sets credentials to access to this repo.
     */
    public JkRepo setCredentials(JkRepoCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    JkRepo mergeCredential(JkRepoCredentials credentials) {
        if (this.credentials == null) {
            this.credentials = credentials;
            return this;
        }
        this.credentials = this.credentials.merge(credentials);
        return this;
    }

    /**
     * @see #setCredentials(JkRepoCredentials)
     */
    public JkRepo setCredentials(String username, String password, String realm) {
        return this.setCredentials(JkRepoCredentials.of(username, password, realm));
    }

    /**
     * @see #setCredentials(JkRepoCredentials)
     */
    public JkRepo setCredentials(String username, String password) {
        String realm = this.getCredentials() == null ? null : this.getCredentials().realm;
        return this.setCredentials(username, password, realm);
    }

    public Map<String, String> getHttpHeaders() {
        return Collections.unmodifiableMap(this.httpHeaders);
    }

    public JkRepo setHttpHeaders(String ...keysAndValues) {
        return setHttpHeaders(JkUtilsIterable.mapOfAny((Object[]) keysAndValues));
    }

    public JkRepo setHttpHeaders(Map<String, String> headers) {
        this.httpHeaders = new HashMap<>(headers);
        return this;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(this.httpHeaders);
    }

    public JkRepoSet toSet() {
        return JkRepoSet.of(this);
    }

    public boolean hasAuthorizationHeader() {
        return this.httpHeaders.containsKey("Authorization");
    }

    /**
     * Retrieves the list of available versions for a given module identifier.
     * This method fetches and parses the associated Maven metadata to extract the versions.
     * If the repository responds with client or server errors, an empty list is returned.
     *
     * @param moduleId a string representing the module identifier in the format group:name
     * @return a list of strings representing the available versions of the specified module in lastest first order
     */
    public List<String> findVersionsOf(String moduleId) {
        JkModuleId jkModuleId = JkModuleId.of(moduleId);
        String path = jkModuleId.getGroup().replace('.', '/') + "/" + jkModuleId.getName() + "/maven-metadata.xml";
        String fullPath = url.toString() + "/" + path;
        JkHttpResponse response = JkHttpRequest.of(fullPath)
                .addHeaders(this.httpHeaders)
                .execute();
        if (response.isClientError()) {
            return Collections.emptyList();
        } else if (response.isServerError()) {
            JkLog.warn("Error while requesting %s. %s", fullPath, response);
            return Collections.emptyList();
        }
        JkDomDocument domDocument = JkDomDocument.parse(response.getBody());
        List<String> result = domDocument.root().get("versioning").children("version").stream()
                .map(JkDomElement::text)
                .collect(Collectors.toCollection(LinkedList::new));
        Collections.reverse(result);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JkRepo jkRepo = (JkRepo) o;
        return url.equals(jkRepo.url);
    }

    public JkRepo copy() {
        JkPublishConfig publishCopy = publishConfig == null ? null : publishConfig.copy();
        JkRepo result = new JkRepo(url, ivyRepo, ivyConfig.copy(), publishCopy );
        result.credentials = credentials;
        return result;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url:").append(url);
        if (httpHeaders.containsKey("Authorization")) {
            sb.append(" - header Authorization: ***");
        }
        //sb.append("credentials: ").append(credentials).append(", ");
        if (this.publishConfig != null) {
            sb.append(" - publish config: ").append(this.publishConfig);
        }
        return sb.toString();
    }

    public String toStringMultiline(String margin) {
        StringBuilder sb = new StringBuilder();
        sb.append(margin).append("url: ").append(url).append("\n");
        String subMargin = "  " + margin;
        if (credentials != null) {
            sb.append(margin).append("credentials: ").append("\n");
            sb.append(subMargin).append("realm: ").append(credentials.getRealm()).append("\n");
            sb.append(subMargin).append("username: ").append(credentials.getUserName()).append("\n");
            String displayedPassword = credentials.password == null ? "-no password set-" : "***";
            sb.append(subMargin).append("password: ").append(displayedPassword).append("\n");
        } else {
            sb.append(margin).append("credentials: none").append("\n");
        }
        if (publishConfig != null) {
            sb.append(margin).append("publish-configuration: ").append("\n");
            sb.append(subMargin).append("checksums: ").append(publishConfig.checksumAlgos).append("\n");
            sb.append(subMargin).append("version-filter: ").append(publishConfig.versionFilter).append("\n");
            sb.append(subMargin).append("sign-artifacts: ").append(publishConfig.signatureRequired).append("\n");
            if (publishConfig.signatureRequired) {
                sb.append(subMargin).append("file-signer: ").append(publishConfig.signer).append("\n");
            }
            sb.append(subMargin).append("unique-snapshot: ").append(publishConfig.uniqueSnapshot);
        } else {
            sb.append(margin).append("publish config: none");
        }
        return sb.toString();
    }

    public void customizeUrlConnection(HttpURLConnection connection) {
        if (credentials != null) {
            String basicAuth = credentials.toAuthorizationHeader();
            if (basicAuth != null) {
                connection.setRequestProperty("Authorization", basicAuth);
            }
        }
        this.httpHeaders.forEach(connection::setRequestProperty);
    }

    private static URL toUrl(String urlOrDir) {
        try {
            return new URL(urlOrDir);
        } catch (final MalformedURLException e) {
            final File file = new File(urlOrDir);
            if (file.isAbsolute()) {
                return JkUtilsPath.toUrl(file);
            } else {
                throw new IllegalArgumentException("<Malformed url " + urlOrDir, e);
            }
        }
    }

    public static final class JkRepoCredentials {

        private final String realm;

        private final String userName;

        private final String password;

        private JkRepoCredentials(String realm, String userName, String password) {
            this.realm = realm;
            this.userName = userName;
            this.password = password;
        }

        public static JkRepoCredentials of(String username, String password) {
            return new JkRepoCredentials(null, username, password);
        }

        public static JkRepoCredentials of(String username, String password, String realm) {
            return new JkRepoCredentials(realm, username, password);
        }

        String encodedBase64() {
            return Base64.getEncoder().encodeToString((JkUtilsString.nullToEmpty(userName) + ":" +
                    JkUtilsString.nullToEmpty(password)).getBytes(StandardCharsets.UTF_8));
        }

        private JkRepoCredentials merge(JkRepoCredentials other) {
            String username = Optional.ofNullable(this.userName).orElse(other.userName);
            String password = Optional.ofNullable(this.password).orElse(other.password);
            String realm = Optional.ofNullable(this.realm).orElse(other.realm);
            return of(username ,password, realm);
        }

        public String getRealm() {
            return realm;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public boolean isEmpty() {
            return JkUtilsString.isBlank(userName) && JkUtilsString.isBlank(password);
        }

        public String toAuthorizationHeader() {
            if (isEmpty()) {
                return null;
            }
            String auth = JkUtilsString.nullToEmpty(userName) + ":" + JkUtilsString.nullToEmpty(password);
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            return "Basic " + auth;
        }

        @Override
        public String toString() {
            return "realm:" + realm + ", userName:" + userName;
        }
    }

    /**
     * Configuration specific to Ivy. This has no effect on Maven repositories
     */
    public static final class JkRepoIvyConfig {

        public static final String DEFAULT_IVY_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision](-[type]).[ext]";

        public static final String DEFAULT_IVY_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";

        private final List<String> artifactPatterns;

        private final List<String> ivyPatterns;

        private JkRepoIvyConfig() {
            this.artifactPatterns = new LinkedList<>(List.of(DEFAULT_IVY_ARTIFACT_PATTERN));
            this.ivyPatterns = new LinkedList<>(List.of(DEFAULT_IVY_IVY_PATTERN));
        }

        public List<String> artifactPatterns() {
            return Collections.unmodifiableList(artifactPatterns);
        }

        public List<String> ivyPatterns() {
            return Collections.unmodifiableList(ivyPatterns);
        }

        public JkRepoIvyConfig emptyArtifactPatterns() {
            this.artifactPatterns.clear();
            return this;
        }

        public JkRepoIvyConfig addArtifactPatterns(String ... patterns) {
            this.artifactPatterns.addAll(Arrays.asList(patterns));
            return this;
        }

        public JkRepoIvyConfig emptyIvyPatterns() {
            this.ivyPatterns.clear();
            return this;
        }

        public JkRepoIvyConfig addIvyPatterns(String ... patterns) {
            this.ivyPatterns.addAll(Arrays.asList(patterns));
            return this;
        }

        private JkRepoIvyConfig copy() {
            JkRepoIvyConfig result = new JkRepoIvyConfig();
            result.artifactPatterns.clear();
            result.artifactPatterns.addAll(artifactPatterns);
            result.ivyPatterns.clear();
            result.ivyPatterns.addAll(ivyPatterns);
            return result;
        }
    }

    /**
     * Configuration specific to publishing.
     */
    public static class JkPublishConfig {

        private final static Predicate<JkVersion> NO_FILTER = new Predicate<JkVersion>() {

            @Override
            public boolean test(JkVersion jkVersion) {
                return true;
            }

            @Override
            public String toString() {
                return "none";
            }
        };

        private Predicate<JkVersion> versionFilter = NO_FILTER;

        private boolean signatureRequired;

        private boolean uniqueSnapshot;

        private Set<String> checksumAlgos = new HashSet<>();

        private JkFileSigner signer;

        /**
         * Returns the filter used for this {@link JkPublishConfig}.
         * Only modules accepted by this filter will pb published on this repo.
         */
        public Predicate<JkVersion> getVersionFilter() {
            return versionFilter;
        }

        public boolean isSignatureRequired() {
            return signatureRequired;
        }

        public boolean isUniqueSnapshot() {
            return uniqueSnapshot;
        }

        public Set<String> getChecksumAlgos() {
            return checksumAlgos;
        }

        public JkFileSigner getSigner() {
            return signer;
        }

        public JkPublishConfig setUniqueSnapshot(boolean uniqueSnapshot) {
            this.uniqueSnapshot = uniqueSnapshot;
            return this;
        }

        public JkPublishConfig setSignatureRequired(boolean signatureRequired) {
            this.signatureRequired = signatureRequired;
            return this;
        }

        public JkPublishConfig setVersionFilter(Predicate<JkVersion> versionFilter) {
            JkUtilsAssert.argument(versionFilter != null, "Filter cannot be null.");
            this.versionFilter = versionFilter;
            return this;
        }

        public JkPublishConfig setChecksumAlgos(String... algos) {
            this.checksumAlgos = JkUtilsIterable.setOf(algos);
            return this;
        }

        public JkPublishConfig setSigner(JkFileSigner signer) {
            this.signer = signer;
            return this;
        }

        private JkPublishConfig copy() {
            JkPublishConfig result = new JkPublishConfig();
            result.signer = signer;
            result.checksumAlgos = new HashSet<>(checksumAlgos);
            result.signatureRequired = signatureRequired;
            result.uniqueSnapshot = uniqueSnapshot;
            result.versionFilter = versionFilter;
            return result;
        }

        @Override
        public String toString() {
            boolean hasVersionFilter = versionFilter != NO_FILTER;
            boolean hasSigner = signer != null;
            return "publishConfig:{" +
                    "versionFilter:" + hasVersionFilter +
                    ", signatureRequired:" + signatureRequired +
                    ", uniqueSnapshot:" + uniqueSnapshot +
                    ", checksumAlgos:" + checksumAlgos +
                    ", signer:" + hasSigner +
                    '}';
        }
    }



}
