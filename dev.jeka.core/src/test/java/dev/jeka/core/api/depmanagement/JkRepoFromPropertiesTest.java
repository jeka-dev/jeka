package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkProperty;
import org.junit.Test;

import static org.junit.Assert.*;

public class JkRepoFromPropertiesTest {

    private static final String URL = "https://myurl";

    @Test
    public void getPublishRepository_default_mavenCentral() {
        clearProps();
        assertEquals(JkRepo.ofMavenCentral().getUrl(), JkRepoFromProperties.getDownloadRepos().getRepos().get(0).getUrl());
    }

    @Test
    public void getPublishRepository_downloadPropOnly_present() {
        clearProps();
        System.setProperty("jeka.repos.download", URL);
        JkRepo repo = JkRepoFromProperties.getDownloadRepos().getRepos().get(0);
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
        JkRepo repo = JkRepoFromProperties.getDownloadRepos().getRepos().get(0);
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
        JkRepo repo = JkRepoFromProperties.getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(realm, repo.getCredentials().getRealm());

        JkRepo repo2 = JkRepoFromProperties.getDownloadRepos().getRepos().get(1);
        assertEquals(myRepoUrl, repo2.getUrl().toString());
        assertEquals(myRepoUsername, repo2.getCredentials().getUserName());
    }

    @Test
    public void getPublishRepository_downloadAliases_ok() {
        clearProps();
        String name = "myname";
        String pwd = "myPwd";
        System.setProperty("jeka.repos.download", JkRepoFromProperties.JEKA_GITHUB_ALIAS
                + ", " + JkRepoFromProperties.JEKA_GITHUB_ALIAS);
        System.setProperty("jeka.repos.jekaGithub.username", name);
        System.setProperty("jeka.repos.jekaGithub.password", pwd);

        JkRepo repo = JkRepoFromProperties.getDownloadRepos().getRepos().get(1);
        JkRepo ghRepo = JkRepo.ofGitHub("jeka-dev", "jeka");
        assertEquals(ghRepo.getUrl(), repo.getUrl());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(ghRepo.getCredentials().getRealm(), repo.getCredentials().getRealm());
    }

    private static void clearProps() {
        JkProperty.find("jeka.repos.").forEach(System::clearProperty);
    }
}