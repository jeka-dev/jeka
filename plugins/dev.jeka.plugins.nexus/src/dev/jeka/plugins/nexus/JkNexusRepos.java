/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.plugins.nexus;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsNet;
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

/**
 * A set of Nexus repos sharing the same base url and credentials.
 */
public class JkNexusRepos {

    static final int DEFAULT_CLOSE_TIMEOUT_SECONDS = 15 * 60; // 15 minutes

    private static final String TASK_NAME = "Closing and releasing repositories";

    private static final long CLOSE_WAIT_INTERVAL_MILLIS = 10_000L;

    private final String baseUrl;

    private final String basicCredential;

    private int readTimeout;

    private int closeTimeout = DEFAULT_CLOSE_TIMEOUT_SECONDS;

    /**
     * A filter to take in account only repositories having specified profile names. If empty, no filter applies.
     */
    private String[] profileNameFilters = new String[0];

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
    public static JkNexusRepos ofRepo(JkRepo repo) {
        JkRepo.JkRepoCredentials repoCredentials = repo.getCredentials();
        URL url = repo.getUrl();
        String baseUrl = url.getProtocol() + "://" + url.getHost();
        if (repo.getCredentials() == null || repo.getCredentials().isEmpty()) {
            JkLog.warn("No credentials set on publish repo " + repo);
            return new JkNexusRepos(baseUrl, null);
        }
        return JkNexusRepos.ofBasicCredentials(baseUrl, repoCredentials.getUserName(), repoCredentials.getPassword());
    }

    /**
     * Convenient method for auto-closing publish repo of the specified
     * project, right after publication.
     *
     * @param profileNames See {@link #setProfileNameFilters(String...)}
     */
    public static void handleAutoRelease(JkMavenPublication mavenPublication, String... profileNames) {
        JkNexusRepos.ofPublishRepo(mavenPublication)
                .setProfileNameFilters(profileNames)
                .autoReleaseAfterPublication(mavenPublication);
    }

    /**
     * Creates a {@link JkNexusRepos} from the publishing repo of the specified {@link JkProject}
     */
    public static JkNexusRepos ofPublishRepo(JkMavenPublication mavenPublication) {
        JkRepo repo = mavenPublication.findFirstNonLocalRepo();
        JkUtilsAssert.argument(repo != null, "No remote publish repo found on mavenPublication " + mavenPublication);
        return JkNexusRepos.ofRepo(repo);
    }

    /**
     * Sets read timeout, in millis, of the http connection. Default is zero, that leads to infinite.
     */
    public JkNexusRepos setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Sets the timeout duration, in seconds, for closing repositories.
     *
     * @param closeTimeout the timeout duration, in milliseconds, to be used for the closing operation.
     */
    public JkNexusRepos setCloseTimeout(int closeTimeout) {
        this.closeTimeout = closeTimeout;
        return this;
    }

    /**
     * Set filters to take in account only repositories having specified profile names. If empty, no filter applies.
     */
    public JkNexusRepos setProfileNameFilters(String... profileNames) {
        this.profileNameFilters = profileNames;
        return this;
    }

    /**
     * Closes then releases staging repositories in OPEN status.
     * Repositories not in OPEN status at time of invoking this method won't be released.
     */
    public void closeAndReleaseOpenRepositories() {
        JkLog.startTask("Nexus: Closing and releasing staged repositories");
        List<JkStagingRepo> stagingRepos = findStagingRepositories();
        JkLog.info("Nexus: Found staging repositories : ");
        stagingRepos.forEach(repo -> JkLog.info(repo.toString()));
        List<String> openRepoIds = stagingRepos.stream()
                .filter(profileNameFilter(profileNameFilters))
                .filter(repo -> JkStagingRepo.Status.OPEN == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        if (profileNameFilters.length != 0) {
            JkLog.info("Nexus: Taking in account repositories with profile name in "
                    + Arrays.asList(profileNameFilters));
        }
        JkLog.info("Nexus: Repositories to close and release : " + openRepoIds);
        close(openRepoIds);
        openRepoIds.forEach(this::waitForClosing);
        release(openRepoIds);
        JkLog.endTask();
    }

    /**
     * Closes repositories in OPEN Status, waits for all repos are closed then releases all repositories..
     */
    public void closeAndRelease() {
        JkLog.startTask("Nexus: Closing and releasing staged repository");
        List<JkStagingRepo> stagingRepos = findStagingRepositories();
        JkLog.info("Found staging repositories : ");
        stagingRepos.forEach(repo -> JkLog.info(repo.toString()));
        List<String> openRepoIds = stagingRepos.stream()
                .filter(profileNameFilter(profileNameFilters))
                .filter(repo -> JkStagingRepo.Status.OPEN == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        if (profileNameFilters.length != 0) {
            JkLog.info("Nexus: Taking in account repositories with profile name in "
                    + Arrays.asList(profileNameFilters));
        }
        close(openRepoIds);
        List<String> closingRepoIds = findStagingRepositories().stream()
                .filter(profileNameFilter(profileNameFilters))
                .filter(repo -> JkStagingRepo.Status.CLOSING == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        closingRepoIds.forEach(this::waitForClosing);
        List<String> closedRepoIds = findStagingRepositories().stream()
                .filter(profileNameFilter(profileNameFilters))
                .filter(repo -> JkStagingRepo.Status.CLOSED == repo.getStatus())
                .map(JkStagingRepo::getId)
                .collect(Collectors.toList());
        JkLog.info("Nexus: Releasing repositories " + closedRepoIds);
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

    public void autoReleaseAfterPublication(JkMavenPublication mavenPublication) {
        mavenPublication.postActions.replaceOrAppend(TASK_NAME, this::closeAndReleaseOpenRepositories);
    }

    private List<JkStagingRepo> doFindStagingRepositories() throws IOException {
        URL url = new URL(baseUrl + "/service/local/staging/profile_repositories");
        HttpURLConnection con = connection(url);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/xml");
        con.setReadTimeout(readTimeout);
        JkUtilsNet.assertResponseOk(con, null);
        JkLog.startTask("Querying staging repositories");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            Document doc = JkUtilsXml.documentFrom(in);
            Element data = JkUtilsXml.directChild(doc.getDocumentElement(), "data");
            List<Element> stagingReposEl = JkUtilsXml.directChildren(data, "stagingProfileRepository");
            return stagingReposEl.stream()
                    .map(JkStagingRepo::fromEl)
                    .collect(Collectors.toList());
        } finally {
            JkLog.endTask();
        }
    }

    private JkStagingRepo doGetRepository(String repositoryId) throws IOException {
        URL url = new URL(baseUrl + "/service/local/staging/repository/" + repositoryId);
        HttpURLConnection con = connection(url);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/xml");
        JkUtilsNet.assertResponseOk(con, null);
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
        JkLog.startTask("Nexus: Sending 'close' command to repositories : " + repositoryIds);
        URL url = new URL(baseUrl + "/service/local/staging/bulk/close");
        HttpURLConnection con = connection(url);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String json = "{\"data\":{\"stagedRepositoryIds\":" + toJsonArray(repositoryIds) + "}}";
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        JkUtilsNet.assertResponseOk(con, json);
        JkLog.endTask();
    }

    private void doRelease(List<String> repositoryIds) throws IOException {
        if (repositoryIds.isEmpty()) {
            JkLog.info("Nexus: No repository to release.");
            return;
        }
        JkLog.startTask("Nexus: Releasing repositories " + repositoryIds);
        URL url = new URL(baseUrl + "/service/local/staging/bulk/promote");
        HttpURLConnection con = connection(url);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String json = "{\"data\":{\"autoDropAfterRelease\":true,\"stagedRepositoryIds\":"
                + toJsonArray(repositoryIds) + "}}";
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        JkUtilsNet.assertResponseOk(con, json);
        JkLog.endTask();
    }

    private void doWaitForClosing(String repositoryId) throws IOException {
        long startMillis = System.currentTimeMillis();
        JkLog.startTask("Nexus: Waiting for repository " + repositoryId + " to be closed. It may take a while ...");
        while (true) {
            if (System.currentTimeMillis() - startMillis > (closeTimeout * 1000L)) {
                throw new IllegalStateException("Nexus: Timeout waiting for repository close.");
            }
            JkUtilsSystem.sleep(CLOSE_WAIT_INTERVAL_MILLIS);
            JkStagingRepo repo = doGetRepository(repositoryId);
            if ("closed".equals(repo.type) && !repo.transitioning) {
                break;
            }
        }
        JkLog.endTask();
    }

    private static Predicate<JkStagingRepo> profileNameFilter(String... profileNames) {
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

    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (Iterator it = items.iterator(); it.hasNext(); ) {
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
                    Boolean.valueOf(JkUtilsXml.directChildText(el, "transitioning")),
                    JkUtilsXml.directChildText(el, "profileName"));
        }

        public Status getStatus() {
            if ("open".equals(type)) {
                return transitioning ? Status.CLOSING : Status.OPEN;
            } else
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
