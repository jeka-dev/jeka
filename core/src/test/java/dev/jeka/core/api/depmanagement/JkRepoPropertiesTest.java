package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkProperties;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JkRepoPropertiesTest {

    private static final String URL = "https://myurl";

    @Test
    public void getPublishRepository_default_mavenCentral() {
        Map<String, String> props = new HashMap<>();
        assertEquals(JkRepo.ofMavenCentral().getUrl(), of(props).getDownloadRepos().getRepos().get(0).getUrl());
    }

    @Test
    public void getPublishRepository_downloadPropOnly_present() {
        Map<String, String> props = new HashMap<>();
        props.put("jeka.repos.download", URL);
        JkRepo repo = of(props).getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
    }

    @Test
    public void getPublishRepository_downloadPropWithCredential_present() {
        String name = "myname";
        String pwd = "myPwd";
        String realm = "myRealm";
        Map<String, String> props = new HashMap<>();
        props.put("jeka.repos.download", URL);
        props.put("jeka.repos.download.username", name);
        props.put("jeka.repos.download.password", pwd);
        props.put("jeka.repos.download.realm", realm);
        props.put("jeka.repos.download.headers.a", "1");
        props.put("jeka.repos.download.headers.bb", "2");
        JkRepo repo = of(props).getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(realm, repo.getCredentials().getRealm());
        Map<String, String> headers = repo.getHttpHeaders();
        assertEquals(2, headers.size());
        assertEquals("1", headers.get("a"));
        assertEquals("2", headers.get("bb"));
    }

    @Test
    public void getPublishRepository_downloadPropWithNames_present() {
        String name = "myname";
        String pwd = "myPwd";
        String realm = "myRealm";
        Map<String, String> props = new HashMap<>();
        props.put("jeka.repos.download", URL + ", myRepo");
        props.put("jeka.repos.download.username", name);
        props.put("jeka.repos.download.password", pwd);
        props.put("jeka.repos.download.realm", realm);

        String myRepoUrl = "https://anotherUrl";
        String myRepoUsername = "myRepoUsername";
        props.put("jeka.repos.myRepo", myRepoUrl);
        props.put("jeka.repos.myRepo.username", myRepoUsername);
        JkRepo repo = of(props).getDownloadRepos().getRepos().get(0);
        assertEquals(URL, repo.getUrl().toString());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(realm, repo.getCredentials().getRealm());

        JkRepo repo2 = of(props).getDownloadRepos().getRepos().get(1);
        assertEquals(myRepoUrl, repo2.getUrl().toString());
        assertEquals(myRepoUsername, repo2.getCredentials().getUserName());
    }

    @Test
    public void testToColumnText() {
        Map<String, String> map = new HashMap<>();
        System.out.println(JkProperties.ofMap(map).toColumnText(1, 1, false).toString());
    }

    @Test
    @Ignore("Does not work in GH action cause name values GH_TOKEN")
    public void getPublishRepository_downloadAliases_ok() {
        String name = "myname";
        String pwd = "myPwd";
        Map<String, String> props = new HashMap<>();
        props.put("jeka.repos.download", JkRepoProperties.JEKA_GITHUB_ALIAS
                + ", " + JkRepoProperties.JEKA_GITHUB_ALIAS);
        props.put("jeka.repos.jekaGithub.username", name);
        props.put("jeka.repos.jekaGithub.password", pwd);

        JkRepo repo = of(props).getDownloadRepos().getRepos().get(1);
        JkRepo ghRepo = JkRepo.ofGitHub("jeka-dev", "jeka");
        assertEquals(ghRepo.getUrl(), repo.getUrl());
        assertEquals(name, repo.getCredentials().getUserName());
        assertEquals(pwd, repo.getCredentials().getPassword());
        assertEquals(ghRepo.getCredentials().getRealm(), repo.getCredentials().getRealm());
    }

    private static JkRepoProperties of(Map<String, String> map) {
        return JkRepoProperties.of(JkProperties.ofMap("test", map));
    }



}