package dev.jeka.core;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class GithubReleaseContentEditor {

    private final String repoFullName;

    private final String branch;

    private final String token;

    GithubReleaseContentEditor(String repoFullName, String branch, String token) {
        this.repoFullName = repoFullName;
        this.branch = branch;
        this.token = token;
    }

    String getReleaseNoteForTag(Path releaseNote, String tag) {
        String rawTagName = JkUtilsString.substringBeforeFirst(tag, ".RELEASE");
        Map<String, String> releaseNoteMap = parseReleaseContents(releaseNote);
        return releaseNoteMap.get(rawTagName);
    }


    void createRelease(String version, String body) {
        String data = getpPostData(version, body);
        String request = requestEdit();
        JkProcess.of("curl", "--data", data, request)
                .setFailOnError(true)
                .setLogCommand(true)
                .setLogOutput(true)
                .run();
    }

    void listRelease() {
        String request = requestList();
        JkProcess.of("curl", request)
                .setFailOnError(true)
                .setLogCommand(true)
                .setLogOutput(true)
                .run();
    }

    private String getpPostData(String version, String body) {
        return String.format("{\"tag_name\": \"%s\",\n" +
                "  \"target_commitish\": \"%s\",\n" +
                "  \"name\": \"%s\",\n" +
                "  \"body\": \"%s\",\n" +
                "  \"draft\": false, " + "   \"prerelease\": false}",
                version, branch, version, body );
    }

    private String requestEdit() {
        return String.format("https://api.github.com/repos/%s/releases?access_token=%s", repoFullName, token);
    }

    private String requestList() {
        return String.format("https://api.github.com/repos/%s/releases?access_token=%s", repoFullName, token);
    }

    private static Map<String, String> parseReleaseContents(Path mdFile) {
        String currentTag = null;
        Map<String, String> result = new TreeMap<>(JkVersion.VERSION_COMPARATOR);
        StringBuilder currentContent = new StringBuilder();
        List<String> lines = JkUtilsPath.readAllLines(mdFile);
        for (String line : lines) {
            String trimedLine = line.trim();
            if (trimedLine.isEmpty()) {
                continue;
            }
            if (line.startsWith("# ")) {
                String releaseName = getReleaseName(line);
                if (releaseName != null) {
                    if (currentTag != null) {
                        result.put(currentTag, currentContent.toString());
                    }
                    currentTag = releaseName;
                    currentContent = new StringBuilder();
                }
            } else if (currentTag != null){
                currentContent.append(line).append("\\r\\n");
            }
        }
        result.put(currentTag, currentContent.toString());
        return  result;
    }

    private static String getReleaseName(String line) {
        if (line.toLowerCase().contains("ongoing")) {
            return null;
        }
        String[] tokens = JkUtilsString.splitTrimmed(line, " ");
        if (tokens.length < 2) {
            return null;
        }
        return tokens[1];
    }

    private void createAllReleases(Path releaseNote) {
        for (Map.Entry<String, String> entry : parseReleaseContents(releaseNote).entrySet()) {
            String releaseName = entry.getKey() + ".RELEASE";
            System.out.println("Creating release " +releaseName);
            String value = entry.getValue();
            createRelease(releaseName, value);
        }
    }

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);

        ///Map<String, String> rc = parseReleaseContents(Paths.get("../release-note.md"));
        //System.out.println(rc);

       // new ReleaseContentEditor("jerkar/jeka", "master", System.getenv("GH_TOKEN"))
                //.listRelease();
                //.pushData("0.8.5.RELEASE", "This is a test");
               ///.createAllReleases(Paths.get("../release-note.md"));
    }

}
