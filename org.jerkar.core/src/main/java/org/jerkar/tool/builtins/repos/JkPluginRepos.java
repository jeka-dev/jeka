package org.jerkar.tool.builtins.repos;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.*;

import java.util.Map;

/**
 * Plugin for defining repositories.
 */
@JkDoc("Provides configured repositories to download or upload artifacts.\n" +
        "  To select a 'download' (respectively a 'publish') repository, this plugin check in order : \n" +
        "    - the 'repos#downloadRepoName' option which designate the name of the configured plugin\n" +
        "    - the 'repos#downloadUrl', 'repos#downloadUsername' and 'repos#downloadPassword' options to instantiate a repo based on these values\n" +
        "    - the 'Repos.download.url', 'Repos.download.username' and 'Repo.download.password' options to instantiate repo based on these values\n" +
        "    - the default repo returned by JkRepo#mavenCentral() for downloading and local repository for publishing.\n" +
        "  To configure a named repository, add following properties into your [Jerkar_user_home]/options.properties file :\n" +
        "    'Repos.[name].url', 'Repos.[value].username' and 'Repos.[of].password'"
)
public class JkPluginRepos extends JkPlugin {

    // ------------------------------ options -------------------------------------------

    @JkDoc("Url for the publish repository.")
    public String publishUrl;

    @JkDoc("Url for the download repository.")
    public String downloadUrl;

    @JkDoc("Username credential for the publish repository (if needed).")
    public String publishUsername;

    @JkDoc("Password for the publish repository (if needed).")
    public String publishPassword;

    @JkDoc("Username credential for the download repository (if needed).")
    public String downloadUsername;

    @JkDoc("Password for the download repository (if needed).")
    public String downloadPassword;

    @JkDoc("Name of the configured repository to use for publishing artifacts. If null or empty, considered as not set.")
    public String publishRepoName;

    @JkDoc("\"Name of the configured repository to use for publishing artifacts. If null or empty, considered as not set.")
    public String downloadRepoName;

    // ----------------------------------------------------------------------------------

    private final JkPluginPgp pgp;

    protected JkPluginRepos(JkBuild build) {
        super(build);
        pgp = build.plugins().get(JkPluginPgp.class);
    }

    public JkPublishRepo publishRepository() {
        final JkPublishRepo result;
        if (!JkUtilsString.isBlank(publishRepoName)) {
            result = JkPublishRepo.of(JkRepoOptionLoader.repoFromOptions(publishRepoName));
        } else if (!JkUtilsString.isBlank(publishUrl)) {
            result = JkPublishRepo.of(JkRepo.of(publishUrl).withCredential(publishUsername, publishPassword));
        } else {
            JkPublishRepo optionRepo = JkRepoOptionLoader.publishRepository();
            result = optionRepo != null ? optionRepo : JkPublishRepo.local();
        }
        return result.withSigner(pgp.get());
    }

    public JkRepo downloadRepository() {
        if (!JkUtilsString.isBlank(downloadPassword)) {
            return JkRepoOptionLoader.repoFromOptions(downloadRepoName);
        }
        if (!JkUtilsString.isBlank(downloadUrl)) {
            return JkRepo.of(publishUrl).withCredential(downloadUsername, downloadPassword);
        }
        return JkRepoOptionLoader.downloadRepository();
    }

    public JkPgp pgpSigner() {
        return pgp.get();
    }

    @JkDoc("Displays active and configured repositories.")
    public void display() {
        StringBuilder sb = new StringBuilder();

        JkRepo download = downloadRepository();
        String downloadPwd = JkUtilsString.isBlank(download.password()) ? "" : downloadPassword.substring(0, 1) + "*******";
        sb.append("Download repository")
                .append("\n  url : " + download.url())
                .append("\n  username : " + download.userName())
                .append("\n  password : " + downloadPwd);

        JkRepo publish = downloadRepository();
        String publishPwd = JkUtilsString.isBlank(publish.password()) ? "" : publishPassword.substring(0, 1) + "*******";
        sb.append("\nPublish repository")
                .append("\n  url : " + publish.url())
                .append("\n  username : " + publish.userName())
                .append("\n  password : " + publishPwd);


        for (Map.Entry<String, String> entry : JkRepoOptionLoader.allRepositoryOptions().entrySet()) {
            sb.append("\n" + entry.getKey() + " : ").append(entry.getValue());
        }
        JkLog.info(sb.toString());
    }



}
