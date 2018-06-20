package org.jerkar.tool;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;

import java.util.HashMap;
import java.util.Map;


/**
 * Provides static methods for defining {@link JkRepos} from {@link JkOptions}.
 */
public class JkRepoOptionLoader {

    /**
     * Returns repository where are published artifacts. By default it
     * takes the repository defined in options <code>repo.publish.url</code>,
     * <code>repo.publish.username</code> and <code>repo.publish.password</code>
     * .
     * <p>
     * You can select another repository defined in option by setting
     * <code>repo.publishname</code> option. So if you want to select the
     * repository defined as <code>repo.myRepo.url</code> in your options, set
     * option <code>repo.publishname=myRepo</code>.
     * <p>
     * This methods returns <code>null</code> if no matching option found
     */
    public static JkPublishRepo publishRepository() {
        final String repoName = JkUtilsObject.firstNonNull(JkOptions.get("Repo.publishName"), "publish");
        return JkPublishRepo.of(repoFromOptions(repoName));
    }

    /**
     * Returns the repositories where are downloaded dependencies. By default it
     * takes the repository defined in options <code>repo.download.url</code>,
     * <code>repo.publish.username</code> and <code>repo.download.password</code>
     * .
     * <p>
     * You can select another repository defined in option by setting
     * <code>repo.downloadname</code> option. So if you want to select the
     * repository defined as <code>repo.myRepo.url</code> in your options, set
     * option <code>repo.downloadname=myRepo</code>.
     * <p>
     * This methods returns an empy repos if no matching options found.
     */
    public static JkRepo downloadRepository() {
        final String repoName = JkUtilsObject.firstNonNull(JkOptions.get("repo.downloadName"), "download");
        return JkUtilsObject.firstNonNull(repoFromOptions(repoName), JkRepo.mavenCentral());
    }

    /**
     * Returns the repositories where are downloaded dependencies needed to run the build.
     */
    public static JkRepo buildRepository() {
        final String repoName = JkUtilsObject.firstNonNull(JkOptions.get("repo.buildName"), "build");
        JkRepo namedRepo = repoFromOptions(repoName);
        return JkUtilsObject.firstNonNull(namedRepo, downloadRepository());
    }

    /**
     * Creates {@link JkRepo} form Jerkar options. the specified repository name
     * will be turned to <code>repo.[repoName].url</code>,
     * <code>repo.[repoName].username</code> and
     * <code>repo.[repoName].password</code> options for creating according
     * repository.
     */
    public static JkRepo repoFromOptions(String repoName) {
        final String optionName = "repo." + repoName + "." + "url";
        final String url = JkOptions.get(optionName);
        if (JkUtilsString.isBlank(url)) {
            return null;
        }
        final String username = JkOptions.get("repo." + repoName + ".username");
        final String password = JkOptions.get("repo." + repoName + ".password");
        return JkRepo.of(url.trim()).withOptionalCredentials(username, password);
    }

    public static Map<String, String> allRepositoryOptions() {
        Map<String, String> options = new HashMap<>();
        JkOptions.getAll().forEach((key, value) -> {
            if (key.startsWith(".repo") && JkUtilsString.endsWithAny(key, ".url", ".username", ".password",
                    ".downloadName", ".publishName", ".buildName")) {
                options.put(key, value);
            }
        });
        return JkOptions.toDisplayedMap(options);
    }

}