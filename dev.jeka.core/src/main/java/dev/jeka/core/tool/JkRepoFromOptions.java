package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.util.HashMap;
import java.util.Map;


/**
 * Provides static methods for creating repositories from standard properties.
 * <p>
 * Basic configuration can be achieved by setting below options.
 * <pre>
 * jeka.repos.download.url=
 * jeka.repos.download.username=    (optional)
 * jeka.repos.download.password=    (optional)
 *
 * jeka.repos.publish.url=
 * jeka.repos.publish.username=     (optional)
 * jeka.repos.publish.password=     (optional)
 * </pre>
 *
 * <p>
 *  If you deal with many repositories, you can override basic setting using named repositories. <br/>
 * <pre>
 * jeka.repos.aRepoName.url=
 * jeka.repos.aRepoName.username=    (optional)
 * jeka.repos.aRepoName.password=    (optional)
 *
 * jeka.repos.anotherRepoName.url=
 * jeka.repos.anotherRepoName.username=    (optional)
 * jeka.repos.anotherRepoName.password=    (optional)
 *
 * jeka.repos.download.name=aRepoName
 *
 * </pre>
 * If  <i>download</i> or <i>publish</i> repo is defined to use a named repo (as jeka.repos.download.name=aRepoName),
 * this takes precedence over basic configuration.
 */
public class JkRepoFromOptions {

    /**
     * Returns repository where are published artifacts. Returns <code>null</code> if no download publish repo is defined.
     */
    public static JkRepo getPublishRepository() {
        String repoName = JkOptions.get("jeka.repos.publish.name");
        if (repoName != null) {
            return getNamedRepo(repoName);
        }
        if (JkOptions.get("jeka.repos.publish.url") != null) {
            return ofPrefix("jeka.repos.publish");
        }
        return JkRepo.ofMavenCentral();
    }

    /**
     * Returns repo from where are downloaded dependencies. Returns Maven central repo if no download repository is defined.
     */
    public static JkRepo getDownloadRepo() {
        String repoName = JkOptions.get("jeka.repos.download.name");
        if (repoName != null) {
            return getNamedRepo(repoName);
        }
        if (JkOptions.get("jeka.repos.download.url") != null) {
            return ofPrefix("jeka.repos.download");
        }
        return JkRepo.ofMavenCentral();
    }

    /**
     * Creates {@link JkRepo} form Jeka options. the specified repository name
     * will be turned to <code>repo.[repoName].url</code>,
     * <code>repo.[repoName].username</code> and
     * <code>repo.[repoName].password</code> options for creating according
     * repository.
     */
    public static JkRepo getNamedRepo(String repoName) {
        String namedRepoOptionPrefix = "jeka.repos." + repoName;
        return ofPrefix(namedRepoOptionPrefix);
    }

    private static JkRepo ofPrefix(String prefix) {
        String url = JkOptions.get(prefix + ".url");
        JkUtilsAssert.argument(url != null, "No option defined for " + prefix + ".url");
        String username = JkOptions.get(prefix + ".username");
        String password = JkOptions.get(prefix + "."+ ".password");
        return JkRepo.of(url.trim()).setCredentials(username, password);
    }

    public static Map<String, String> allRepositoryOptions() {
        Map<String, String> result = new HashMap<>();
        JkOptions.getAll().forEach((key, value) -> {
            if (key.startsWith("jeka.repos.")) {
                result.put(key, value);
            }
        });
        return JkOptions.toDisplayedMap(result);
    }

}