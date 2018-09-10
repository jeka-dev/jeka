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

    /**
     * URL of the Maven central repository.
     */
    public static final String MAVEN_CENTRAL_URL = "http://repo1.maven.org/maven2";

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
    public static final String MAVEN_OSSRH_DEPLOY_RELEASE = "https://oss.sonatype.org/service/publishLocalOnly/staging/deploy/maven2/";

    /**
     * URL of the OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static final String MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT = "https://oss.sonatype.org/content/groups/public/";

    /**
     * URL of the JCenter ivy repository.
     */
    public static final String JCENTERL_URL = "https://jcenter.bintray.com";

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
     */
    public static JkRepo of(String url) {
        if (url.toLowerCase().startsWith("ivy:")) {
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
     * Creates a Maven repository having the specified file location.
     */
    public static JkRepo ofIvy(Path dir) {
        return new JkRepo(JkUtilsPath.toUrl(dir), null, JkRepoIvyConfig.of(), JkPublishConfig.of());
    }

    /**
     * Creates the Maven central repository.
     */
    public static JkRepo mavenCentral() {
        return of(MAVEN_CENTRAL_URL);
    }

    /**
     * Creates an OSSRH repository for both deploying snapshot and download artifacts.
     */
    public static JkRepo mavenOssrhDownloadAndDeploySnapshot(String jiraId, String jiraPassword) {
        return of(MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT)
                .with(JkRepoCredential.of(jiraId, jiraPassword, "Sonatype Nexus Repository Manager"));
    }

    /**
     * Creates an OSSRH repository for deploying released artifacts.
     */
    public static JkRepo mavenOssrhDeployRelease(String jiraId, String jiraPassword) {
        return of(MAVEN_OSSRH_DEPLOY_RELEASE).with(
                JkRepoCredential.of(jiraId, jiraPassword, "Sonatype Nexus Repository Manager"));
    }

    /**
     * Creates a OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static JkRepo mavenOssrhPublicDownload() {
        return of(MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT);
    }

    /**
     * Creates a JCenter repository.
     */
    public static JkRepo mavenJCenter() {
        return of(JCENTERL_URL);
    }



    /**
     * Creates a Maven repository for publishing locally under <code></code>[USER HOME]/.jerkar/publish</code> folder.
     */
    public static JkRepo local() {
        final Path file = JkLocator.jerkarUserHomeDir().resolve("maven-publish-dir");
        return JkRepo.ofMaven(file);
    }

    /**
     * Returns the url of this repository.
     */
    public final URL url() {
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
    public JkRepoIvyConfig ivyConfig() {
        return this.ivyConfig;
    }

    public boolean isIvyRepo() {
        return this.ivyConfig != null;
    }

    /**
     * Returns the realm of this repository.
     */
    public final JkRepoCredential credential() {
        return credential;
    }

    public JkPublishConfig publishConfig() {
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

    public JkRepoSet asSet() {
        return JkRepoSet.of(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JkRepo other = (JkRepo) obj;
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + url + ")";
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

        public String realm() {
            return realm;
        }

        public String userName() {
            return userName;
        }

        public String password() {
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

        private final boolean needSignature;

        private final boolean uniqueSnapshot;

        private JkPublishConfig(JkPublishFilter filter, boolean needSignature, boolean uniqueSnapshot) {
            super();
            this.filter = filter;;
            this.uniqueSnapshot = uniqueSnapshot;
            this.needSignature = needSignature;
        }

        public static JkPublishConfig of() {
            return new JkPublishConfig(JkPublishFilter.ACCEPT_ALL, false, false);
        }

        /**
         * Creates a {@link JkPublishConfig} for publishing snapshot version on the specified {@link JkRepo}.
         * Release versions are not publishable on this {@link JkPublishConfig}
         */
        public static JkPublishConfig ofSnapshotOnly(boolean uniqueSnapshot) {
            return new JkPublishConfig(JkPublishFilter.ACCEPT_SNAPSHOT_ONLY, false, uniqueSnapshot);
        }

        /**
         * Creates a {@link JkPublishConfig} for publishing non-snapshot projectVersion on the specified {@link JkRepo}.
         * Snapshot versions are not publishable on this {@link JkPublishConfig}
         */
        public static JkPublishConfig ofReleaseOnly(boolean needSignature) {
            return new JkPublishConfig(JkPublishFilter.ACCEPT_RELEASE_ONLY, needSignature, false);
        }

        /**
         * Returns the filter used for this {@link JkPublishConfig}.
         * Only modules accepted by this filter will pb published on this repo.
         */
        public JkPublishFilter filter() {
            return filter;
        }

        public boolean isNeedSignature() {
            return needSignature;
        }

        public boolean isUniqueSnapshot() {
            return uniqueSnapshot;
        }

        public JkPublishConfig withUniqueSnapshot(boolean uniqueSnapshot) {
            return new JkPublishConfig(this.filter, this.needSignature, uniqueSnapshot);
        }

        public JkPublishConfig withNeedSignature(boolean needSignature) {
            return new JkPublishConfig(this.filter, needSignature, uniqueSnapshot);
        }
    }

}
