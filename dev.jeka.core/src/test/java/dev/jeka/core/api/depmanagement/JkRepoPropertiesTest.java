package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkProperties;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JkRepoPropertiesTest {

    private static final String URL = "https://myurl";

    @Test
    public void getPublishRepository_default_mavenCentral() {
        clearProps();
        assertEquals(JkRepo.ofMavenCentral().getUrl(), systemRepoProps().getDownloadRepos().getRepos().get(0).getUrl());
    }

    @Test
    public void getPublishRepository_downloadPropOnly_present() {
        clearProps();
        System.setProperty("jeka.repos.download", URL);
        JkRepo repo = systemRepoProps().getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
    }

    @Test
    public void getPublishRepository_downloadPropWithCredential_present() {
        clearProps();
        String name = "myname";
        String pwd = "myPwd";
        String realm = "myRealm";
        System.setProperty("jeka.repos.download", URL);
        System.setProperty("jeka.repos.download.username", name);
        System.setProperty("jeka.repos.download.password", pwd);
        System.setProperty("jeka.repos.download.realm", realm);
        JkRepo repo = systemRepoProps().getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(realm, repo.getCredentials().getRealm());
    }

    @Test
    public void getPublishRepository_downloadPropWithNames_present() {
        clearProps();
        String name = "myname";
        String pwd = "myPwd";
        String realm = "myRealm";
        System.setProperty("jeka.repos.download", URL + ", myRepo");
        System.setProperty("jeka.repos.download.username", name);
        System.setProperty("jeka.repos.download.password", pwd);
        System.setProperty("jeka.repos.download.realm", realm);

        String myRepoUrl = "https://anotherUrl";
        String myRepoUsername = "myRepoUsername";
        System.setProperty("jeka.repos.myRepo", myRepoUrl);
        System.setProperty("jeka.repos.myRepo.username", myRepoUsername);
        JkRepo repo = systemRepoProps().getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(realm, repo.getCredentials().getRealm());

        JkRepo repo2 = systemRepoProps().getDownloadRepos().getRepos().get(1);
        assertEquals(myRepoUrl, repo2.getUrl().toString());
        assertEquals(myRepoUsername, repo2.getCredentials().getUserName());
    }

    @Test
    @Ignore("Does not work in GH action cause name values GH_TOKEN")
    public void getPublishRepository_downloadAliases_ok() {
        clearProps();
        String name = "myname";
        String pwd = "myPwd";
        System.setProperty("jeka.repos.download", JkRepoProperties.JEKA_GITHUB_ALIAS
                + ", " + JkRepoProperties.JEKA_GITHUB_ALIAS);
        System.setProperty("jeka.repos.jekaGithub.username", name);
        System.setProperty("jeka.repos.jekaGithub.password", pwd);

        JkRepo repo = systemRepoProps().getDownloadRepos().getRepos().get(1);
        JkRepo ghRepo = JkRepo.ofGitHub("jeka-dev", "jeka");
        assertEquals(ghRepo.getUrl(), repo.getUrl());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(ghRepo.getCredentials().getRealm(), repo.getCredentials().getRealm());
    }

    private static JkRepoProperties systemRepoProps() {
        return JkRepoProperties.of(JkProperties.ofSystemProperties());
    }

    private static void clearProps() {
        JkProperties.ofSystemProperties().find("jeka.repos.").forEach(System::clearProperty);
    }
}