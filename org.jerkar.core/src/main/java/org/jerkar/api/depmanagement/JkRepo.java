package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

/**
 * An abstract repository that can be either a Maven nor an Ivy repository.
 */
public abstract class JkRepo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * URL ofMany the Maven central repository.
     */
    public static final URL MAVEN_CENTRAL_URL = toUrl("http://repo1.maven.org/maven2");

    /**
     * URL ofMany the OSSRH repository for both deploying snapshot and download artifacts.
     */
    public static final URL MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT = toUrl("https://oss.sonatype.org/content/repositories/snapshots/");

    /**
     * URL for the OSSRH repository for downloading released artifacts.
     */
    public static final URL MAVEN_OSSRH_DOWNLOAD_RELEASE = toUrl("https://oss.sonatype.org/content/repositories/releases/");

    /**
     * URL ofMany the OSSRH repository for deploying released artifacts.
     */
    public static final URL MAVEN_OSSRH_DEPLOY_RELEASE = toUrl("https://oss.sonatype.org/service/publishLocally/staging/deploy/maven2/");

    /**
     * URL ofMany the OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static final URL MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT = toUrl("https://oss.sonatype.org/content/groups/public/");

    /**
     * URL ofMany the JCenter ivy repository.
     */
    public static final URL JCENTERL_URL = toUrl("https://jcenter.bintray.com");

    /**
     * Creates a repository or <code>null</code> according the url is <code>null</code> or not.
     */
    public static JkRepo ofOptional(String url, String userName, String password) {
        if (JkUtilsString.isBlank(url)) {
            return null;
        }
        return of(url).withCredential(userName, password);
    }



    /**
     * Returns the first repository not <code>null</code> from the specified ones.
     */
    public static JkRepo firstNonNull(JkRepo... repos) {
        for (final JkRepo repo : repos) {
            if (repo != null) {
                return repo;
            }
        }
        throw new NullPointerException("All repository arguments are null.");
    }

    /**
     * Creates Maven repository having the specified url.
     */
    public static JkMavenRepository maven(String url) {
        return new JkMavenRepository(toUrl(url), null, null, null);
    }

    /**
     * Creates Maven repository having the specified url.
     */
    public static JkMavenRepository maven(URL url) {
        return new JkMavenRepository(url, null, null, null);
    }

    /**
     * Creates a Maven repository having the specified file location.
     */
    public static JkMavenRepository maven(Path dir) {
        return new JkMavenRepository(JkUtilsPath.toUrl(dir), null, null, null);
    }

    /**
     * Creates the Maven central repository.
     */
    public static JkRepo mavenCentral() {
        return maven(JkMavenRepository.MAVEN_CENTRAL_URL.toString());
    }

    /**
     * Creates an OSSRH repository for both deploying snapshot and download artifacts.
     */
    public static JkRepo mavenOssrhDownloadAndDeploySnapshot(String jiraId, String jiraPassword) {
        return maven(JkMavenRepository.MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT.toString())
                .withCredential(jiraId, jiraPassword)
                .withRealm("Sonatype Nexus Repository Manager");
    }

    /**
     * Creates an OSSRH repository for deploying released artifacts.
     */
    public static JkRepo mavenOssrhDeployRelease(String jiraId, String jiraPassword) {
        return maven(JkMavenRepository.MAVEN_OSSRH_DEPLOY_RELEASE.toString()).withCredential(
                jiraId, jiraPassword).withRealm("Sonatype Nexus Repository Manager");
    }

    /**
     * Creates a OSSRH repository for downloading both snapshot and released artifacts.
     */
    public static JkRepo mavenOssrhPublicDownload() {
        return maven(JkMavenRepository.MAVEN_OSSRH_PUBLIC_DOWNLOAD_RELEASE_AND_SNAPSHOT);
    }

    /**
     * Creates a JCenter repository.
     */
    public static JkRepo mavenJCenter() {
        return maven(JkMavenRepository.JCENTERL_URL.toString());
    }

    /**
     * Returns this repository as a publish repository.
     */
    public JkPublishRepo asPublishRepo() {
        return JkPublishRepo.of(this);
    }

    /**
     * Returns this repository as a list ofMany publish repositories (having a single element).
     */
    public JkPublishRepos asPublishRepos() {
        return JkPublishRepos.of(JkPublishRepo.of(this));
    }

    /**
     * Returns this repository as a publish repository for snapshot artifacts.
     */
    public JkPublishRepo asPublishSnapshotRepo() {
        return JkPublishRepo.ofSnapshot(this);
    }

    /**
     * Returns this repository as a list ofMany repositories (containing a single element).
     */
    public JkRepos asRepos() {
        return JkRepos.of(this);
    }

    /**
     * Returns this repository as a publish repository for released artifacts.
     */
    public JkPublishRepo asPublishReleaseRepo() {
        return JkPublishRepo.ofRelease(this);
    }

    /**
     * Creates a repository having the specified url. If the repository is an Ivy repository
     * than the url should start with <code>ivy:</code> as <code>ivy:http://myrepolocation</code>.
     */
    public static JkRepo of(String url) {
        if (url.toLowerCase().startsWith("ivy:")) {
            return JkRepo.ivy(url.substring(4));
        }
        return JkRepo.maven(url);
    }

    /**
     * Creates a Ivy repository located at the specified url.
     */
    public static JkRepo.JkIvyRepository ivy(URL url) {
        return new JkIvyRepository(url, null, null, null, null, null);
    }

    /**
     * Creates a Ivy repository located at the specified file location.
     */
    public static JkRepo.JkIvyRepository ivy(Path file) {
        return ivy(JkUtilsPath.toUrl(file));
    }

    /**
     * Creates a Ivy repository located at the specified file location or url depending the
     * specified infoString stands for a url or a file location.
     */
    public static JkRepo.JkIvyRepository ivy(String urlOrDir) {
        if (urlOrDir.toLowerCase().startsWith("ivy:")) {
            return JkRepo.ivy(urlOrDir.substring(4));
        }
        return ivy(toUrl(urlOrDir));
    }

    private final URL url;

    private final String userName;

    private final String realm;

    private final String password;

    private JkRepo(URL url, String realm, String userName, String password) {
        this.url = url;
        this.realm = realm;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Returns the url ofMany this repository.
     */
    public final URL url() {
        return url;
    }

    /**
     * Returns the realm ofMany this repository.
     */
    public final String realm() {
        return realm;
    }

    /**
     * Returns the username used to connect to this repository
     */
    public final String userName() {
        return userName;
    }

    /**
     * Returns the password used to connect to this repository
     */
    public final String password() {
        return password;
    }

    /**
     * Returns <code>true</code> if some credential has been set on this repository.
     */
    public boolean hasCredentials() {
        return !JkUtilsString.isBlank(userName);
    }

    /**
     * Returns a copy ofMany this repository but having the specified credentials. If the
     * username credential is <code>null</code> the method return this unchanged repository.
     */
    public final JkRepo withOptionalCredentials(String userName, String password) {
        if (JkUtilsString.isBlank(userName)) {
            return this;
        }
        return this.withCredential(userName, password);
    }

    /**
     * Returns a list ofMany 2 repositories containing this one and the specified one.
     */
    public JkRepos and(JkRepo other) {
        return JkRepos.of(this, other);
    }

    /**
     * Returns a copy ofMany this repository but with the specified realm.
     */
    public abstract JkRepo withRealm(String realm);

    /**
     * Returns a copy ofMany this repository but having the specified credentials.
     */
    public abstract JkRepo withCredential(String username, String password);

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

    /**
     * A Maven repository.
     */
    public static final class JkMavenRepository extends JkRepo {

        private static final long serialVersionUID = 1L;

        private JkMavenRepository(URL url, String realm, String userName, String password) {
            super(url, realm, userName, password);
        }

        /**
         * Returns a copy ofMany this repository but having the specified credentials.
         */
        @Override
        public JkRepo withCredential(String username, String password) {
            return new JkMavenRepository(this.url(), this.realm(), username, password);
        }

        /**
         * Returns a copy ofMany this repository but having the specified realm.
         */
        @Override
        public JkRepo withRealm(String realm) {
            return new JkMavenRepository(this.url(), realm, this.userName(), this.password());
        }

    }

    /**
     * An Ivy repository.
     */
    public static final class JkIvyRepository extends JkRepo {

        private static final long serialVersionUID = 1L;

        private final List<String> artifactPatterns;

        private final List<String> ivyPatterns;

        private static final String DEFAULT_IVY_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision](-[type]).[ext]";

        private static final String DEFAULT_IVY_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";

        private JkIvyRepository(URL url, String realm, String username, String password,
                List<String> artifactPatterns, List<String> ivyPatterns) {
            super(url, realm, username, password);
            this.artifactPatterns = artifactPatterns;
            this.ivyPatterns = ivyPatterns;
        }

        /**
         * Returns a copy ofMany this repository but having the specified artifact patterns.
         */
        public JkIvyRepository artifactPatterns(String... patterns) {
            return new JkIvyRepository(this.url(), this.realm(), this.userName(), this.password(),
                    Collections.unmodifiableList(Arrays.asList(patterns)), ivyPatterns);
        }

        /**
         * Returns a copy ofMany this repository but having the specified Ivy patterns.
         */
        public JkIvyRepository ivyPatterns(String... patterns) {
            return new JkIvyRepository(this.url(), this.realm(), this.userName(), this.password(),
                    artifactPatterns, Collections.unmodifiableList(Arrays.asList(patterns)));
        }

        /**
         * Returns the list ofMany artifact patterns for this Ivy repository.
         */
        public List<String> artifactPatterns() {
            if (this.artifactPatterns == null) {
                return JkUtilsIterable.listOf(DEFAULT_IVY_ARTIFACT_PATTERN);
            }
            return artifactPatterns;
        }

        /**
         * Returns a list ofMany Ivy patterns for this Ivy repository.
         */
        public List<String> ivyPatterns() {
            if (this.ivyPatterns == null) {
                return JkUtilsIterable.listOf(DEFAULT_IVY_IVY_PATTERN);
            }
            return ivyPatterns;
        }

        @Override
        public JkRepo withCredential(String username, String password) {
            return new JkIvyRepository(this.url(), this.realm(), username, password,
                    this.artifactPatterns, this.ivyPatterns);
        }

        @Override
        public JkRepo withRealm(String realm) {
            return new JkIvyRepository(this.url(), realm, this.userName(), this.password(),
                    this.artifactPatterns, this.ivyPatterns);
        }

    }

}
