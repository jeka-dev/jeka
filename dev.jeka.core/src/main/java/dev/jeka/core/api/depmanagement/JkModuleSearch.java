package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomXPath;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsHttp;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JkModuleSearch {

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

    private JkModuleSearch() {
    }

    public static JkModuleSearch of() {
        return new JkModuleSearch();
    }

    public static JkModuleSearch of(JkRepo repo) {
        JkModuleSearch result = of();
        String repoUrl = repo.getUrl().toString();
        String resolvedUrl = REPO_API_URL.get(repoUrl);
        if (resolvedUrl != null) {
            result.setApiUrl(resolvedUrl);
        } else {
            result.repoBaseUrl = repoUrl;
        }
        return result;
    }

    public JkModuleSearch setApiUrl(String url) {
        this.apiUrl = url;
        return this;
    }



    public JkModuleSearch setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public JkModuleSearch setGroupOrNameCriteria(String groupOrNameCriteria) {
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
                JkLog.trace("No API found at " + url + " (404).");
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
        return result;
    }

    private List<String> search(String apiUrl) throws IOException {
        String query = createQuery();
        String encodedQuery = URLEncoder.encode(query);
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
        JkUtilsHttp.assertResponseOk(con, null);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            Document domDoc = JkUtilsXml.documentFrom(in);
            JkDomDocument doc = JkDomDocument.of(domDoc);
            doc.print(System.out);
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




}
