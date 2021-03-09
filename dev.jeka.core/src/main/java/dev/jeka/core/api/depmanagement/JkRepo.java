package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsFile;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Hold configuration necessary to instantiate download or upload repository
 */
public final class JkRepo {

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

    private final URL url;

    private JkRepoCredentials credentials;

    private final JkRepoIvyConfig ivyConfig = new JkRepoIvyConfig(this);

    private final JkPublishConfig publishConfig = new JkPublishConfig(this);

    private JkRepo(URL url) {
        this.url = url;
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
        return new JkRepo(toUrl(url));
    }

    /**
     * Creates a Maven repository having the specified file location.
     */
    public static JkRepo of(Path dir) {
        return new JkRepo(JkUtilsPath.toUrl(dir));
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
                .setCredentials(JkRepoCredentials.of(jiraId, jiraPassword, "Sonatype Nexus Repository Manager"))
                .getPublishConfig()
                    .setUniqueSnapshot(false)
                    .setVersionFilter(JkVersion::isSnapshot).__;
    }

    /**
     * Creates an OSSRH repository for deploying released artifacts.
     */
    public static JkRepo ofMavenOssrhDeployRelease(String jiraId, String jiraPassword,
                                                   UnaryOperator<Path> signer) {
        return of(MAVEN_OSSRH_DEPLOY_RELEASE)
                .setCredentials(jiraId, jiraPassword, "Sonatype Nexus Repository Manager")
                .getPublishConfig()
                    .setSignatureRequired(true)
                    .setVersionFilter(version -> !version.isSnapshot())
                    .setSigner(signer)
                    .setChecksumAlgos("md5", "sha1").__;
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
     * Creates a Maven repository for publishing locally under <code></code>[USER HOME]/.jeka/publish</code> folder.
     */
    public static JkRepo ofLocal() {
        final Path file = JkLocator.getJekaUserHomeDir().resolve("maven-publish-dir");
        return JkRepo.of(file);
    }

    /**
     * Returns the url of this repository.
     */
    public final URL getUrl() {
        return url;
    }

    /**
     * Returns configuration specific to Ivy repository. Returns <code>null</code> if this configuration stands
     * for a Maven repository.
     */
    public JkRepoIvyConfig getIvyConfig() {
        return this.ivyConfig;
    }

    /**
     * Returns the getRealm of this repository.
     */
    public final JkRepoCredentials getCredentials() {
        return credentials;
    }

    public JkPublishConfig getPublishConfig() {
        return publishConfig;
    }

    /**
     * Sets credentials to access to this repo.
     */
    public JkRepo setCredentials(JkRepoCredentials credentials) {
        this.credentials = credentials;
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
        return this.setCredentials(username, password, null);
    }

    public JkRepoSet toSet() {
        return JkRepoSet.of(this);
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
     * Configuration specific to Ivy. This has no effect on Maven repositories
     */
    public static final class JkRepoIvyConfig {

        public static final String DEFAULT_IVY_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision](-[type]).[ext]";

        public static final String DEFAULT_IVY_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";

        public final JkRepo __;

        private final List<String> artifactPatterns;

        private final List<String> ivyPatterns;

        private JkRepoIvyConfig(JkRepo parent) {
            this.__ = parent;
            this.artifactPatterns = JkUtilsIterable.listOf(DEFAULT_IVY_ARTIFACT_PATTERN);
            this.ivyPatterns = JkUtilsIterable.listOf(DEFAULT_IVY_IVY_PATTERN);
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
    }

    /**
     * Configuration specific to publishing.
     */
    public static class JkPublishConfig {

        public final JkRepo __;

        private Predicate<JkVersion> versionFilter = jkVersion -> true;

        private boolean signatureRequired;

        private boolean uniqueSnapshot;

        private Set<String> checksumAlgos = new HashSet<>();

        private UnaryOperator<Path> signer;

        private JkPublishConfig(JkRepo parent) {
            __ = parent;
        }

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

        public UnaryOperator<Path> getSigner() {
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

        public JkPublishConfig setSigner(UnaryOperator<Path> signer) {
            this.signer = signer;
            return this;
        }

    }

}
