import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkDoc;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// =============================================================================
// TriageKBean — entry point (jeka triage#run)
// =============================================================================

/**
 * JeKa KBean — AI triage agent for GitHub issues.
 *
 * Reads a newly opened issue via environment variables (injected by
 * GitHub Actions), classifies it with Claude API, applies labels,
 * and posts a tailored first response.
 *
 * Run locally:
 *   ANTHROPIC_API_KEY=sk-ant-... GITHUB_TOKEN=ghp_... \
 *   REPO=jeka-dev/jeka ISSUE_NUMBER=42 \
 *   ISSUE_TITLE="..." ISSUE_BODY="..." ISSUE_AUTHOR="..." \
 *   jeka triage: run
 */
class Triage extends KBean {

    @JkDoc("Classify the issue, add labels, and post a first response.")
    public void run() {
        JkLog.info("=== JeKa AI Issue Triage ===");

        // 1. Load issue context from environment
        IssueContext issue = IssueContext.fromEnv();
        JkLog.info("Issue #%d: %s (by @%s)".formatted(
                issue.number(), issue.title(), issue.author()));

        // 2. Classify with Claude
        JkLog.info("Classifying issue with Claude...");
        TriageResult triage = ClaudeTriageClient.classify(issue);
        JkLog.info("Classification: %s (confidence: %s)".formatted(
                triage.category(), triage.confidence()));

        // 3. Apply GitHub label
        GitHubClient github = new GitHubClient(issue.repo());
        github.addLabel(issue.number(), triage.githubLabel());
        JkLog.info("Label applied: " + triage.githubLabel());

        // 4. Post first response
        github.postComment(issue.number(), triage.response());
        JkLog.info("Comment posted successfully.");

        JkLog.info("=== Triage complete ===");
    }
}

// =============================================================================
// IssueContext — issue data loaded from environment variables
// =============================================================================

/**
 * Immutable snapshot of a GitHub issue, loaded from environment variables
 * injected by the GitHub Actions workflow.
 */
record IssueContext(
        String repo,
        int    number,
        String title,
        String body,
        String author
) {

    /** Load issue context from environment variables. */
    static IssueContext fromEnv() {
        return new IssueContext(
                requireEnv("REPO"),
                Integer.parseInt(requireEnv("ISSUE_NUMBER")),
                requireEnv("ISSUE_TITLE"),
                System.getenv().getOrDefault("ISSUE_BODY", ""),
                requireEnv("ISSUE_AUTHOR")
        );
    }

    /** Summarised representation for prompt injection (max ~500 chars). */
    String toPromptSnippet() {
        String truncatedBody = body.length() > 400
                ? body.substring(0, 400) + "..."
                : body;
        return """
                Title : %s
                Author: @%s
                Body  :
                %s
                """.formatted(title, author, truncatedBody);
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank())
            throw new IllegalStateException("Required environment variable not set: " + key);
        return value;
    }
}

// =============================================================================
// TriageResult — Claude's structured classification output
// =============================================================================

/**
 * Result returned by Claude after classifying a GitHub issue.
 *
 * @param category    One of: bug | feature_request | question | documentation | other
 * @param confidence  One of: high | medium | low
 * @param response    Ready-to-post Markdown comment for the issue
 */
record TriageResult(
        String category,
        String confidence,
        String response
) {

    /** Maps the category to the corresponding GitHub label slug. */
    String githubLabel() {
        return switch (category) {
            case "bug"             -> "bug";
            case "feature_request" -> "enhancement";
            case "question"        -> "question";
            case "documentation"   -> "documentation";
            default                -> "triage";
        };
    }
}

// =============================================================================
// ClaudeTriageClient — calls Anthropic API, parses JSON response
// =============================================================================

/**
 * Calls the Anthropic Claude API to classify a GitHub issue and
 * generate a first response. Uses only java.net.http — no extra deps.
 */
class ClaudeTriageClient {

    private static final String API_URL     = "https://api.anthropic.com/v1/messages";
    private static final String MODEL       = "claude-sonnet-4-20250514";
    private static final String API_VERSION = "2023-06-01";

    private static final String JEKA_DOCS     = "https://jeka-dev.github.io/jeka/";
    private static final String JEKA_EXAMPLES = "https://github.com/jeka-dev/jeka/tree/master/samples";

    // ── Public API ────────────────────────────────────────────────────────────

    /** Classify {@code issue} and return a {@link TriageResult}. */
    static TriageResult classify(IssueContext issue) {
        String apiKey      = requireEnv("ANTHROPIC_API_KEY");
        String rawResponse = callClaude(apiKey, buildPrompt(issue));
        return parseResponse(rawResponse);
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private static String buildPrompt(IssueContext issue) {
        return """
                You are the AI triage assistant for JeKa, a modern Java build tool.
                Analyze the following GitHub issue and respond ONLY with a valid JSON object.

                Issue:
                %s

                JSON schema (respond with ONLY this, no markdown, no explanation):
                {
                  "category":   "<bug|feature_request|question|documentation|other>",
                  "confidence": "<high|medium|low>",
                  "response":   "<ready-to-post Markdown comment in English>"
                }

                Rules for the response field:
                - Greet the author by their GitHub handle (@%s).
                - Be friendly, technical, and concise.
                - For bugs: ask for JeKa version, OS, and a minimal reproduction.
                - For questions: answer if possible or point to %s or %s.
                - For feature requests: thank them and ask for a use-case description.
                - Always end with: "A maintainer will follow up shortly."
                - Never make up API details you are unsure about.
                """.formatted(issue.toPromptSnippet(), issue.author(), JEKA_DOCS, JEKA_EXAMPLES);
    }

    // ── HTTP call ─────────────────────────────────────────────────────────────

    private static String callClaude(String apiKey, String prompt) {
        String requestBody = """
                {
                  "model": "%s",
                  "max_tokens": 1000,
                  "messages": [
                    { "role": "user", "content": %s }
                  ]
                }
                """.formatted(MODEL, jsonString(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key",         apiKey)
                .header("anthropic-version", API_VERSION)
                .header("content-type",      "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200)
                throw new RuntimeException("Claude API error %d: %s"
                        .formatted(response.statusCode(), response.body()));

            return extractText(response.body());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call Claude API", e);
        }
    }

    // ── JSON helpers (stdlib only) ────────────────────────────────────────────

    /** Extract the text field from Anthropic's response envelope. */
    private static String extractText(String json) {
        int start = json.indexOf("\"text\":");
        if (start == -1) throw new RuntimeException("Unexpected Claude response: " + json);
        int quoteStart = json.indexOf('"', start + 7) + 1;
        int quoteEnd   = findClosingQuote(json, quoteStart);
        return unescapeJson(json.substring(quoteStart, quoteEnd));
    }

    /** Parse Claude's JSON output into a {@link TriageResult}. */
    private static TriageResult parseResponse(String text) {
        String clean = text.replaceAll("(?s)```json\\s*|```", "").trim();
        return new TriageResult(
                extractJsonString(clean, "category"),
                extractJsonString(clean, "confidence"),
                extractJsonString(clean, "response")
        );
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx == -1) throw new RuntimeException("Missing key '" + key + "' in: " + json);
        int colon      = json.indexOf(':', keyIdx + search.length());
        int quoteStart = json.indexOf('"', colon + 1) + 1;
        int quoteEnd   = findClosingQuote(json, quoteStart);
        return unescapeJson(json.substring(quoteStart, quoteEnd));
    }

    private static int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '\\') { i++; continue; }
            if (s.charAt(i) == '"')  return i;
        }
        throw new RuntimeException("Unterminated JSON string starting at " + start);
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n",  "\n")
                .replace("\\t",  "\t")
                .replace("\\\\", "\\");
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private static String requireEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Environment variable not set: " + key);
        return v;
    }
}

// =============================================================================
// GitHubClient — posts comments and labels via GitHub REST API v3
// =============================================================================

/**
 * Minimal GitHub REST API v3 client.
 * Supports posting issue comments and adding labels.
 * Uses only java.net.http — no extra dependencies.
 */
class GitHubClient {

    private static final String API_BASE = "https://api.github.com";

    private final String     repo;
    private final String     token;
    private final HttpClient http;

    GitHubClient(String repo) {
        this.repo  = repo;
        this.token = requireEnv("GITHUB_TOKEN");
        this.http  = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Post a Markdown comment on the given issue. */
    void postComment(int issueNumber, String markdownBody) {
        String url  = "%s/repos/%s/issues/%d/comments".formatted(API_BASE, repo, issueNumber);
        post(url, "{\"body\": %s}".formatted(jsonString(markdownBody)), 201);
    }

    /** Add a label to the given issue (label must already exist in the repo). */
    void addLabel(int issueNumber, String label) {
        String url  = "%s/repos/%s/issues/%d/labels".formatted(API_BASE, repo, issueNumber);
        post(url, "{\"labels\": [\"%s\"]}".formatted(label), 200);
    }

    private void post(String url, String jsonBody, int expectedStatus) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization",       "Bearer " + token)
                .header("Accept",              "application/vnd.github+json")
                .header("X-GitHub-Api-Version","2022-11-28")
                .header("Content-Type",        "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != expectedStatus)
                throw new RuntimeException("GitHub API error %d for %s: %s"
                        .formatted(response.statusCode(), url, response.body()));

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call GitHub API: " + url, e);
        }
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private static String requireEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Environment variable not set: " + key);
        return v;
    }
}