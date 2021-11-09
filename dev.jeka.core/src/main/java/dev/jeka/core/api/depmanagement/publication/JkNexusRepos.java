package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JkNexusRepos {

    private static final long CLOSE_TIMEOUT_MILLIS = 15 * 60 * 1000L;

    private static final long  CLOSE_WAIT_INTERVAL_MILLIS = 10_000L;

    private final String baseUrl;

    private final String basicCredential;

    private JkNexusRepos(String baseUrl, String basicCredential) {
        this.baseUrl = baseUrl;
        this.basicCredential = basicCredential;
    }

    private static JkNexusRepos ofBasicCredentials(String baseUrl, String userName, String password) {
        byte[] basicCredential = Base64.getEncoder().encode((userName + ":" + password)
                .getBytes(StandardCharsets.UTF_8));
        return new JkNexusRepos(baseUrl, new String(basicCredential));
    }

    /**
     * Creates a {@link JkNexusRepos} from information contained in the specified repo, meaning baseUrl and credentials.
     */
    public static JkNexusRepos ofUrlAndCredentials(JkRepo repo) {
        JkRepo.JkRepoCredentials repoCredentials = repo.getCredentials();
        URL url = repo.getUrl();
        String baseUrl = url.getProtocol() + "://" + url.getHost();
        return JkNexusRepos.ofBasicCredentials(baseUrl, repoCredentials.getUserName(), repoCredentials.getPassword());
    }

    /**
     * Closes then releases staging repositories in OPEN status.
     * Repositories not in OPEN status at time of invoking this method won't be released.
     * @param profileNames a filter to take in account only repositories having specified profile names. If empty, no filter applies.
     */
    public void closeAndReleaseOpenRepositories(String ...profileNames) {
        JkLog.startTask("Closing and releasing staged repositories");
        List<JkStagingRepo> stagingRepos = findStagingRepositories();
        JkLog.info("Found staging repositories : ");
        stagingRepos.forEach(repo -> JkLog.info(repo.toString()));
        List<String> openRepoIds = stagingRepos.stream()
                .filter(profileNameFilter(profileNames))
                .filter(repo -> JkStagingRepo.Status.OPEN == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        if (profileNames.length != 0) {
            JkLog.info("Taking in account repositories with profile name in " + Arrays.asList(profileNames));
        }
        JkLog.info("Repositories to close and release : " + openRepoIds);
        close(openRepoIds);
        openRepoIds.forEach(this::waitForClosing);
        release(openRepoIds);
        JkLog.endTask();
    }

    /**
     * Closes repositories in OPEN Status, waits for all repos are closed then releases all repositories.
     * @param profileNames a filter to take in account only repositories having specified profile names. If empty, no filter applies.
     */
    public void closeAndRelease(String ...profileNames) {
        JkLog.startTask("Closing and releasing staged repository");
        List<JkStagingRepo> stagingRepos = findStagingRepositories();
        JkLog.info("Found staging repositories : ");
        stagingRepos.forEach(repo -> JkLog.info(repo.toString()));
        List<String> openRepoIds = stagingRepos.stream()
                .filter(profileNameFilter(profileNames))
                .filter(repo -> JkStagingRepo.Status.OPEN == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        if (profileNames.length != 0) {
            JkLog.info("Taking in account repositories with profile name in " + Arrays.asList(profileNames));
        }
        JkLog.info("Repositories to close : " + openRepoIds);
        close(openRepoIds);
        List<String> closingRepoIds = stagingRepos.stream()
                .filter(profileNameFilter(profileNames))
                .filter(repo -> JkStagingRepo.Status.CLOSING == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        JkLog.info("Repositories to wait for been closed : " + closingRepoIds);
        closingRepoIds.forEach(this::waitForClosing);
        List<String> closedRepoIds = stagingRepos.stream()
                .filter(profileNameFilter(profileNames))
                .filter(repo -> JkStagingRepo.Status.CLOSED == repo.getStatus() || JkStagingRepo.Status.CLOSING == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        JkLog.info("Repositories to release : " + closedRepoIds);
        release(closedRepoIds);
        JkLog.endTask();
    }

    public List<JkStagingRepo> findStagingRepositories() {
        try {
            return doFindStagingRepositories();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void close(List<String> repositoryIds) {
        try {
            doClose(repositoryIds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void waitForClosing(String repositoryId) {
        try {
            doWaitForClosing(repositoryId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void release(List<String> repositoryIds) {
        try {
            doRelease(repositoryIds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<JkStagingRepo> doFindStagingRepositories() throws IOException {
        URL url = new URL(baseUrl + "/service/local/staging/profile_repositories");
        HttpURLConnection con = connection(url);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/xml");
        assertResponseOk(con, null);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            Document doc = JkUtilsXml.documentFrom(in);
            Element data = JkUtilsXml.directChild(doc.getDocumentElement(), "data");
            List<Element> stagingReposEl = JkUtilsXml.directChildren(data, "stagingProfileRepository");
            return stagingReposEl.stream()
                    .map(JkStagingRepo::fromEl)
                    .collect(Collectors.toList());
        }
    }

    private JkStagingRepo doGetRepository(String repositoryId) throws IOException {
        URL url = new URL(baseUrl + "/service/local/staging/repository/" + repositoryId);
        HttpURLConnection con = connection(url);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/xml");
        assertResponseOk(con, null);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            Document doc = JkUtilsXml.documentFrom(in);
            Element root = doc.getDocumentElement();
            return JkStagingRepo.fromEl(root);
        }
    }

    private void doClose(List<String> repositoryIds) throws IOException {
        if (repositoryIds.isEmpty()) {
            JkLog.info("No staging repository to close.");
            return;
        }
        JkLog.startTask("Closing repositories " + repositoryIds);
        URL url = new URL(baseUrl + "/service/local/staging/bulk/close");
        HttpURLConnection con = connection(url);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String json = "{\"data\":{\"stagedRepositoryIds\":" + toJsonArray(repositoryIds) + "}}";
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        assertResponseOk(con, json);
        JkLog.endTask();
    }

    private void doRelease(List<String> repositoryIds) throws IOException {
        JkLog.startTask("Releasing repositories " + repositoryIds);
        URL url = new URL(baseUrl + "/service/local/staging/bulk/promote");
        HttpURLConnection con = connection(url);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String json = "{\"data\":{\"autoDropAfterRelease\":true,\"stagedRepositoryIds\":"
                + toJsonArray(repositoryIds) + "}}";
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        assertResponseOk(con, json);
        JkLog.endTask();
    }

    private void doWaitForClosing(String repositoryId) throws IOException {
        long startMillis = System.currentTimeMillis();
        JkLog.startTask("Waiting for repository " + repositoryId + " been closed. It make take a while ...");
        while (true) {
            if (System.currentTimeMillis() - startMillis > CLOSE_TIMEOUT_MILLIS) {
                throw new IllegalStateException("Timeout waiting for repository close.");
            }
            JkUtilsSystem.sleep(CLOSE_WAIT_INTERVAL_MILLIS);
            JkStagingRepo repo = doGetRepository(repositoryId);
            if ("closed".equals(repo.type)) {
                break;
            }
        }
        JkLog.endTask();
    }

    private static Predicate<JkStagingRepo> profileNameFilter(String ... profileNames) {
        if (profileNames.length == 0) {
            return repo -> true;
        }
        return repo -> Arrays.asList(profileNames).contains(repo.profileName);
    }

    private HttpURLConnection connection(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "basic " + basicCredential);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(true);
        return con;
    }

    private void assertResponseOk(HttpURLConnection con, String body) throws IOException {
        int code = con.getResponseCode();
        if (code >= 400) {
            InputStream inputStream = con.getErrorStream();
            if (inputStream == null) {
                throw new IllegalStateException("Request " + con.getRequestMethod() + " " +  con.getURL()
                        + " failed with status code " + code + "\nRequest body : " + body);
            }
            try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new IllegalStateException("Request " + con.getURL() + " failed with status code " + code + "\n"
                        + "Request body : " + body + "\n"
                        + "Response body : " + response);
            }
        }
    }

    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (Iterator it = items.iterator(); it.hasNext();) {
            sb.append("\"").append(it.next()).append("\"");
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static class JkStagingRepo {

        enum Status {
            OPEN, CLOSING, CLOSED
        }

        private final String id;
        private final long updatedTimestamp;
        private final String url;
        private final String type; // open,closed
        private final boolean transitioning;
        private final String profileName;

        private JkStagingRepo(String id, long updatedTimestamp, String url, String type, boolean transitioning, String profileName) {
            this.id = id;
            this.updatedTimestamp = updatedTimestamp;
            this.url = url;
            this.type = type;
            this.transitioning = transitioning;
            this.profileName = profileName;
        }

        private static JkStagingRepo fromEl(Element el) {
            return new JkStagingRepo(
                    JkUtilsXml.directChildText(el, "repositoryId"),
                    Long.valueOf(JkUtilsXml.directChildText(el, "updatedTimestamp")),
                    JkUtilsXml.directChildText(el, "repositoryURI"),
                    JkUtilsXml.directChildText(el, "type"),
                    Boolean.valueOf(JkUtilsXml.directChildText(el,"transitioning")),
                    JkUtilsXml.directChildText(el, "profileName"));
        }

        public Status getStatus() {
            if ("open".equals(type)) {
                return transitioning ? Status.CLOSING : Status.OPEN;
            }
            return Status.CLOSED;
        }

        public String getId() {
            return id;
        }

        public long getUpdatedTimestamp() {
            return updatedTimestamp;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }

        public boolean isTransitioning() {
            return transitioning;
        }

        public String getProfileName() {
            return profileName;
        }

        @Override
        public String toString() {
            return "JkStagingRepo{" +
                    "id='" + id + '\'' +
                    ", updatedTimestamp=" + updatedTimestamp +
                    ", url='" + url + '\'' +
                    ", type='" + type + '\'' +
                    ", transitioning=" + transitioning +
                    ", profileName='" + profileName + '\'' +
                    '}';
        }
    }




}
