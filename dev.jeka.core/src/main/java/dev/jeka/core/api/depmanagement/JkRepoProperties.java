package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Arrays;
import java.util.List;
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
 *
 * jeka.repos.publish=  (url)
 * jeka.repos.publish.username=     (optional)
 * jeka.repos.publish.password=     (optional)
 * jeka.repos.publish.realm=     (optional)
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

    public static JkRepoProperties of(JkProperties properties) {
        return new JkRepoProperties(properties);
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

    private JkRepo getRepo(String propertyName, String nameOrUrl) {
        if (isUrl(nameOrUrl)) {
            return JkRepo.of(nameOrUrl).setCredentials(getCredentials(propertyName));
        }
        return getRepoByName(nameOrUrl);
    }

    private JkRepo.JkRepoCredentials getCredentials(String prefix) {
        String userName = properties.get(prefix + ".username");
        String password = properties.get(prefix + ".password");
        String realm = properties.get(prefix + ".realm");
        return JkRepo.JkRepoCredentials.of(userName, password, realm);
    }

    private List<String> downloadUrlOrNames() {
        return urlOrNames(properties.get("jeka.repos.download"));
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