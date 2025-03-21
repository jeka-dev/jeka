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

package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomXPath;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsXml;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpression;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class JkCoordinateSearch {

    private static final Map<String, String> REPO_API_URL = new HashMap<>();

    private static XPathExpression orgMavenSearchSpellSuggestionXpath;

    private static XPathExpression orgMmavenSearchIdsXpath;

    static {
        REPO_API_URL.put(JkRepo.MAVEN_CENTRAL_URL, "https://search.maven.org/solrsearch/select");
    }

    /**
     * Nexus repositories generally provide a rest API for querying in lucene style.
     * This is the path where this api is exposed by default.
     */
    public static final String LUCENE_API_STANDARD_PATH = "/service/local/lucene/search";

    private String apiUrl;

    private String repoBaseUrl;

    private int timeout = 5000;

    private String groupOrNameCriteria;

    private JkCoordinateSearch() {
    }

    public static JkCoordinateSearch of() {
        return new JkCoordinateSearch();
    }

    public static JkCoordinateSearch of(JkRepo repo) {
        JkCoordinateSearch result = of();
        String repoUrl = repo.getUrl().toString();
        String resolvedUrl = REPO_API_URL.get(repoUrl);
        if (resolvedUrl != null) {
            result.setApiUrl(resolvedUrl);
        } else {
            result.repoBaseUrl = repoUrl;
        }
        return result;
    }

    public JkCoordinateSearch setApiUrl(String url) {
        this.apiUrl = url;
        return this;
    }

    /**
     * Sets the timeout for the search operation.
     *
     * @param timeout the timeout duration in milliseconds
     */
    public JkCoordinateSearch setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets the group or name criteria for the search.
     *
     * @param groupOrNameCriteria the group or name pattern to be used as a search criterion.
     *                            Example: 'org.springframework.boot:spring-boot-dependencies::'
     * @return the current instance of {@code JkCoordinateSearch} for method chaining
     */
    public JkCoordinateSearch setGroupOrNameCriteria(String groupOrNameCriteria) {
        this.groupOrNameCriteria = groupOrNameCriteria;
        return this;
    }

    public List<String> search()  {
        if (JkUtilsString.isBlank(this.groupOrNameCriteria)) {
            return Collections.emptyList();
        }
        String baseCandidtate = this.repoBaseUrl;
        List<String> result;
        String url;
        do  {
            url = getApiUrl(baseCandidtate);
            try {
                result = search(url);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (result == null) {
                JkLog.verbose("No API found at %s (404).", url);
                baseCandidtate = getParentUrl(baseCandidtate);
                if (baseCandidtate == null) {
                    break;
                }
            }
        } while (result == null);
        if (result == null) {
            throw new IllegalStateException("No search API found on repo " + url);
        } else {
            if (repoBaseUrl != null && !REPO_API_URL.containsKey(repoBaseUrl)) {
                REPO_API_URL.put(repoBaseUrl, url);
            }
        }
        if (showVersion()) {
            result = result.stream()
                    .sorted(new VersionedComparator().reversed())
                    .collect(Collectors.toList());
        }
        return result;
    }

    private List<String> search(String apiUrl) throws IOException {
        String query = createQuery();
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String versionParam = showVersion() ? "&core=gav" : "";
        URL aUrl = new URL(apiUrl + "?q=" + encodedQuery + versionParam + "&wt=xm&rows=200");
        HttpURLConnection con = (HttpURLConnection) aUrl.openConnection();
        con.setRequestMethod("GET");
        con.setReadTimeout(timeout);
        con.setRequestProperty("accept", "application/xml");
        con.setAllowUserInteraction(true);
        int code = con.getResponseCode();
        if (code == 404) {
            return null;
        }
        JkUtilsNet.assertResponseOk(con, null);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            Document domDoc = JkUtilsXml.documentFrom(in);
            JkDomDocument doc = JkDomDocument.of(domDoc);
            return toResult(doc);
        }
    }

    private String getApiUrl(String baseCandidate) {
        if (apiUrl != null) {
            return  apiUrl;
        }
        if (JkUtilsString.isBlank(baseCandidate)) {
            throw new IllegalStateException("No url search or repository has been defined.");
        }
        return getApiUrlForLuceneFromRepoUrl(baseCandidate);
    }

    private static String getParentUrl(String url) {
        String trimmedUrl = removeTrailingSlash(url);
        String result = JkUtilsString.substringBeforeLast(trimmedUrl, "/");
        if (result.endsWith("/")) {
            return null; // url is http://
        }
        return result;
    }

    private static String getApiUrlForLuceneFromRepoUrl(String repoBaseUrl) {
        repoBaseUrl = removeTrailingSlash(repoBaseUrl);
        return repoBaseUrl + LUCENE_API_STANDARD_PATH;
    }

    private static String removeTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length()-1);
        }
        return url;
    }

    private String createQuery() {
        int separatorCount = JkUtilsString.countOccurrence(this.groupOrNameCriteria, ':');
        String[] tokens = this.groupOrNameCriteria.split(":");
        if (separatorCount == 0) {
            return tokens[0];
        }
        if (separatorCount == 1) {
            return "g:" + tokens[0];
        }
        if (separatorCount >= 2) {
            return "g:" + tokens[0] + " AND a:" + tokens[1];
        }
        return String.join("", tokens);
    }

    private boolean showVersion() {
        int separatorCount = JkUtilsString.countOccurrence(this.groupOrNameCriteria, ':');
        return separatorCount >=2;
    }

    private List<String> toResult(JkDomDocument domDocument) {
        String rootName = domDocument.root().getW3cElement().getTagName();
        if ("searchNGResponse".equals(rootName)) {
            return toResultFromLuceneSearch(domDocument);
        } else if ("response".equals(rootName)) {
            return toResultFromOrgMavenSearch(domDocument, showVersion());
        } else {
            domDocument.print(JkLog.getOutPrintStream());
            throw new IllegalStateException("Can not parse search response.");
        }

    }

    // see https://central.sonatype.org/search/rest-api-guide/
    private static List<String> toResultFromOrgMavenSearch(JkDomDocument domDocument, boolean showVersion) {
        List<String> result = domDocument.root().xPath(orgMavenSearchCentralIds()).stream()
                .map(el -> {
                    String base = el.text();
                    if (!showVersion) {
                        return base;
                    }
                    return base;
                })
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            result = domDocument.root().xPath(orgMavenSearchSpellSuggest()).stream()
                    .map(el -> el.text())
                    .collect(Collectors.toList());
        }
        return result;
    }

    private static List<String> toResultFromLuceneSearch(JkDomDocument domDocument) {
        return domDocument.root().child("data").children("artifact").stream()
                .map(el -> {
                    return el.get("groupId").text() + ":" + el.get("artifactId").text() + ":" + el.get("version").text();
                })
                .collect(Collectors.toList());
    }

    private static XPathExpression orgMavenSearchSpellSuggest() {
        if (orgMavenSearchSpellSuggestionXpath == null) {
            orgMavenSearchSpellSuggestionXpath = JkDomXPath.compile(
                    "/response/lst[@name='spellcheck']/lst[@name='suggestions']/lst/arr[@name='suggestion']/str");
        }
        return orgMavenSearchSpellSuggestionXpath;
    }

    private static XPathExpression orgMavenSearchCentralIds() {
        if (orgMmavenSearchIdsXpath == null) {
            orgMmavenSearchIdsXpath = JkDomXPath.compile("/response/result[@name='response']/doc/str[@name='id']");
        }
        return orgMmavenSearchIdsXpath;
    }

    private static class VersionedComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            String v1 = JkUtilsString.substringAfterLast(o1, ":");
            String v2 = JkUtilsString.substringAfterLast(o2, ":");
            return JkVersion.of(v1).compareTo(JkVersion.of(v2));
        }
    }




}
