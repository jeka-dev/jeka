package org.jerkar.tool;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Options about publication on repositories.
 */
public class JkRepoOptions {

    @JkDoc("Tell if the sources must be published.")
    public boolean publishSources = true;

    @JkDoc("Tell if the test classes must be published.")
    public boolean publishTests = false;

    @JkDoc("Force to publish in local repository.")
    public boolean publishLocally = false;

    @JkDoc("Sign the artifacts with PGP prior publishing.")
    public boolean signPublishedArtifacts = false;

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


    /**
     * Creates {@link JkRepos} from Jerkar options. The specified repository
     * name will be turned to <code>repo.[repoName].url</code>,
     * <code>repo.[repoName].username</code> and
     * <code>repo.[repoName].password</code> options for creating according
     * repository.
     *
     * You can specify severals urls by using comma separation in
     * <code>repo.[repoName].url</code> option value but credential will remain
     * the same for all returned repositories.
     */
    public static JkRepos reposFromOptions(String repoName) {
        final String urls = JkOptions.get("repo." + repoName + "." + "url");
        JkRepos result = JkRepos.empty();
        if (JkUtilsString.isBlank(urls)) {
            return result;
        }
        final String username = JkOptions.get("repo." + repoName + ".username");
        final String password = JkOptions.get("repo." + repoName + ".password");
        for (final String url : urls.split(",")) {
            result = result.and(JkRepo.of(url.trim()).withOptionalCredentials(username, password));
        }
        return result;
    }

    /**
     * Returns the repositories where are published artifacts. By default it
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
    public JkPublishRepos publishRepositories() {
        if (publishLocally) {
            return JkPublishRepos.local();
        }
        final String repoName = JkUtilsObject.firstNonNull(JkOptions.get("repo.publishname"), "publish");
        JkRepo repo = repoFromOptions(repoName);
        if (repo == null) {
            return null;
        }
        return JkPublishRepos.of(repo.asPublishRepo());
    }

    /**
     * Returns the repositories where are downloaded artifacts. By default it
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
    public JkRepos downloadRepositories() {
        final String repoName = JkUtilsObject.firstNonNull(JkOptions.get("repo.downloadname"), "download");
        return reposFromOptions(repoName);
    }

    /**
     * Returns a pgp signer according options :
     * <ul>
     *     <li>pgp.pubring : file containing public keys</li>
     *     <li>pgp.secring : file containing private keys</li>
     *     <li>pgp.secretKeyPassword : secret for the file containing private keys</li>
     * </ul>
     */
    public JkPgp pgpSigner() {
        return JkPgp.of(JkOptions.getAll());
    }

}