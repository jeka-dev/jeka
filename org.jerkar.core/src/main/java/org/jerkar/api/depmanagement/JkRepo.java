package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.*;

/**
 * Hold configuration necessary to instantiate download or upload repository
 */
public final class JkRepo implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String LOCAL_NAME = "local";

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

    /**
     * URL of the JCenter ivy repository.
     */
    public static final String JCENTERL_URL = "https://jcenter.bintray.com";

    private static final String IVY_PREFIX = "ivy:";

    private final URL url;

    private final JkRepoCredential credential;

    private final JkRepoIvyConfig ivyConfig;

    private final JkPublishConfig publishConfig;


    private JkRepo(URL url, JkRepoCredential credential, JkRepoIvyConfig ivyConfig, JkPublishConfig publishConfig) {
        this.url = url;
        this.credential = credential;
        this.ivyConfig = ivyConfig;
        this.publishConfig = publishConfig;
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
            return new JkRepo(toUrl(url.substring(4)), null, JkRepoIvyConfig.of(), JkPublishConfig.of());
        }
        return new JkRepo(toUrl(url), null, null, JkPublishConfig.of());
    }

    /**
     * Creates a Maven repository having the specified file location.
     */
    public static JkRepo ofMaven(Path dir) {
        return new JkRepo(JkUtilsPath.toUrl(dir), null, null, JkPublishConfig.of());
    }

    /**
     * Creates a Ivy repository having the specified file location.
     */
    public static JkRepo ofIvy(Path dir) {
        return new JkRepo(JkUtilsPath.toUrl(dir), null, JkRepoIvyConfig.of(), JkPublishConfig.of());
    }

    /**
     * Creates the Maven central repository.
     */
    public static JkRepo ofMavenCentral() {
        return of(MAVEN_CENTRAL_URL);
    }

    /**
     * Creates an OSSRH repository for both deploying snapshot and download artifacts.
     */
    public static JkRepo ofMavenOssrhDownloadAndDeploySnapshot(String jiraId, String jiraPassword) {
        return of(MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT)
                .with(JkRepoCredential.of(jiraId, jiraPassword, "Sonatype Nexus Repository Manager"))
                .with(JkPublishConfig.ofSnapshotOnly(false));
    }

    /**
     * Creates an OSSRH repository for deploying released artifacts.
     */
    public static JkRepo ofMavenOssrhDeployRelease(String jiraId, String jiraPassword) {
        return of(MAVEN_OSSRH_DEPLOY_RELEASE)
                .with(JkRepoCredential.of(jiraId, jiraPassword, "Sonatype Nexus Repository Manager"))
                .with(JkPublishConfig.ofReleaseOnly(true).withChecksumAlgos("md5", "sha1"));
    }

    /**
     * Creates a OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static JkRepo ofMavenOssrhPublicDownload() {
        return of(MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT);
    }

    /**
     * Creates a JCenter repository.
     */
    public static JkRepo ofMavenJCenter() {
        return of(JCENTERL_URL);
    }



    /**
     * Creates a Maven repository for publishing locally under <code></code>[USER HOME]/.jerkar/publish</code> folder.
     */
    public static JkRepo ofLocal() {
        final Path file = JkLocator.getJerkarUserHomeDir().resolve("maven-publish-dir");
        return JkRepo.ofMaven(file);
    }

    /**
     * Returns the url of this repository.
     */
    public final URL getUrl() {
        return url;
    }

    /**
     * Returns <code>true</code> if some credential has been set on this repository.
     */
    public boolean hasCredentials() {
        return credential != null;
    }

    /**
     * Returns configuration specific to Ivy repository. Returns <code>null</code> if this configuration stands
     * for a Maven repository.
     */
    public JkRepoIvyConfig getIvyConfig() {
        return this.ivyConfig;
    }

    public boolean isIvyRepo() {
        return this.ivyConfig != null;
    }

    /**
     * Returns the getRealm of this repository.
     */
    public final JkRepoCredential getCredential() {
        return credential;
    }

    public JkPublishConfig getPublishConfig() {
        return publishConfig;
    }

    /**
     * Returns a copy of this repository but having the specified credentials.
     */
    public JkRepo with(JkRepoCredential credential) {
        return new JkRepo(this.url, credential, this.ivyConfig, this.publishConfig);
    }

    public JkRepo withOptionalCredentials(String username, String password) {
        if (!JkUtilsString.isBlank(username)) {
            return with(JkRepoCredential.of(username, password, null));
        }
        return this;
    }

    public JkRepo with(JkPublishConfig publishConfig) {
        return new JkRepo(this.url, this.credential, this.ivyConfig, publishConfig);
    }

    public JkRepoSet toSet() {
        return JkRepoSet.of(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkRepo jkRepo = (JkRepo) o;
        return url.equals(jkRepo.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    private static URL toUrl(String urlOrDir) {
        try {
            return new URL(urlOrDir);
        } catch (final MalformedURLException e) {
            final File file = new File(urlOrDir);
            if (file.isAbsolute()) {
                return JkUtilsFile.toUrl(file);
            } else {
                throw new IllegalArgumentException("<Malformed url " + urlOrDir, e);
            }
        }
    }


    public static final class JkRepoCredential {

        private final String realm;

        private final String userName;

        private final String password;

        private JkRepoCredential(String realm, String userName, String password) {
            this.realm = realm;
            this.userName = userName;
            this.password = password;
        }

        public static JkRepoCredential of(String username, String password) {
            return new JkRepoCredential(null, username, password);
        }

        public static JkRepoCredential of(String username, String password, String realm) {
            return new JkRepoCredential(realm, username, password);
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
    }

    /**
     * Configuration specific to Ivy.
     */
    public static final class JkRepoIvyConfig {

        private static final long serialVersionUID = 1L;

        public static final String DEFAULT_IVY_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision](-[type]).[ext]";

        public static final String DEFAULT_IVY_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";

        private final List<String> artifactPatterns;

        private final List<String> ivyPatterns;

        private JkRepoIvyConfig(List<String> artifactPatterns, List<String> ivyPatterns) {
            this.artifactPatterns = Collections.unmodifiableList(artifactPatterns);
            this.ivyPatterns = Collections.unmodifiableList(ivyPatterns);
        }

        public static JkRepoIvyConfig of(List<String> artifactPatterns, List<String> ivyPatterns) {
            return new JkRepoIvyConfig(artifactPatterns, ivyPatterns);
        }

        public static JkRepoIvyConfig of() {
            return new JkRepoIvyConfig(JkUtilsIterable.listOf(DEFAULT_IVY_ARTIFACT_PATTERN),
                    JkUtilsIterable.listOf(DEFAULT_IVY_IVY_PATTERN));
        }


        public List<String> artifactPatterns() {
            return artifactPatterns;
        }

        public List<String> ivyPatterns() {
            return ivyPatterns;
        }
    }

    /**
     * Configuration specific to publishing.
     */
    public static class JkPublishConfig implements Serializable {

        private static final long serialVersionUID = 1L;

        private final JkPublishFilter filter;

        private final boolean signatureRequired;

        private final boolean uniqueSnapshot;

        private final Set<String> checksumAlgos;

        private JkPublishConfig(JkPublishFilter filter, boolean signatureRequired, boolean uniqueSnapshot,
                                Set<String> checksumAlgos) {
            super();
            this.filter = filter;
            this.uniqueSnapshot = uniqueSnapshot;
            this.signatureRequired = signatureRequired;
            this.checksumAlgos = Collections.unmodifiableSet(new HashSet<>(checksumAlgos));
        }

        public static JkPublishConfig of() {
            return new JkPublishConfig(JkPublishFilter.ACCEPT_ALL, false, false,
                    Collections.emptySet());
        }

        /**
         * Creates a {@link JkPublishConfig} for publishing snapshot version on the specified {@link JkRepo}.
         * Release versions are not publishable on this {@link JkPublishConfig}
         */
        public static JkPublishConfig ofSnapshotOnly(boolean uniqueSnapshot) {
            return new JkPublishConfig(JkPublishFilter.ACCEPT_SNAPSHOT_ONLY, false, uniqueSnapshot,
                    Collections.emptySet());
        }

        /**
         * Creates a {@link JkPublishConfig} for publishing non-snapshot version on the specified {@link JkRepo}.
         * Snapshot versions are not publishable on this {@link JkPublishConfig}
         */
        public static JkPublishConfig ofReleaseOnly(boolean needSignature) {
            return new JkPublishConfig(JkPublishFilter.ACCEPT_RELEASE_ONLY, needSignature, false,
                    Collections.emptySet());
        }

        /**
         * Returns the filter used for this {@link JkPublishConfig}.
         * Only modules accepted by this filter will pb published on this repo.
         */
        public JkPublishFilter getFilter() {
            return filter;
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

        public JkPublishConfig withUniqueSnapshot(boolean uniqueSnapshot) {
            return new JkPublishConfig(this.filter, this.signatureRequired, uniqueSnapshot, this.checksumAlgos);
        }

        public JkPublishConfig withNeedSignature(boolean needSignature) {
            return new JkPublishConfig(this.filter, needSignature, uniqueSnapshot, this.checksumAlgos);
        }

        public JkPublishConfig withFilter(JkPublishFilter filter) {
            return new JkPublishConfig(filter, this.signatureRequired, this.uniqueSnapshot, this.checksumAlgos);
        }

        public JkPublishConfig withChecksumAlgos(String... algos) {
            return new JkPublishConfig(this.filter, this.signatureRequired, this.uniqueSnapshot, JkUtilsIterable.setOf(algos));
        }
    }

}
