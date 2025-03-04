/*
 * Copyright 2014-2025  the original author or authors.
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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkDoc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class Github  {

    @JkDoc("Github token injected bby the pipeline")
    public String ghToken;

    @JkDoc("Publish a Release description on Github. The description is extracted from the release-note.md file " +
            "which describe content of all releases.")
    public void publishGhRelease() throws IOException {
        JkGit git = JkGit.of();
        if ("master".equals(git.getCurrentBranch())) {
            JkLog.verbose("Current branch is not master. Don't release.");
            return;  // only publish release from master branch
        }
        if (git.isWorkspaceDirty()) {
            JkLog.verbose("Git workspace is dirty. Don't release.");
            return;  // only publish release from master branch
        }
        String releaseNumber = git.extractSuffixFromLastCommitMessage("Release:");
        if (releaseNumber == null) {
            JkLog.verbose("No release tag found. Don't publish.");
            return;
        }
        git.tagAndPush(releaseNumber);  // Tag the repo

        // Send a rest POST in JDK8 old style
        URL url = new URL("https://github.com/repos/jeka-dev/jeka/releases");
        HttpURLConnection con =(HttpURLConnection) url.openConnection();
        con.setRequestProperty("Accept", "application/vnd.github+json");
        con.setRequestProperty("Authorization", "Bearer " + ghToken);
        con.setRequestProperty("X-GitHub-Api-Version",  "2022-11-28" );
        String body = postBBody(releaseNumber, extractReleaseNote(releaseNumber));
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int status = con.getResponseCode();
        if (status != 201) {
            JkLog.warn("Release publication on Github failed. Status code = " + status);
        }
    }

    private static String extractReleaseNote(String version) {
        Path releaseNoteFile = Paths.get("../release-note.md");
        StringBuilder sb = new StringBuilder();
        boolean startFound = false;
        for (String line : JkUtilsPath.readAllLines(releaseNoteFile)) {
            String versionHeader = versionHeader(line);
            if (startFound && versionHeader != null) {   // This means that is the header of the previous version. Stop here.
                return sb.toString();
            }
            if (!startFound && version.equals(versionHeader)) {
                startFound = true;
                continue;
            }
            if (!startFound) {
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            sb.append(line.trim()).append("\n");
        }
        return sb.toString();
    }

    private static String versionHeader(String line) {
        List<String> words = Arrays.asList(line.split(" "));
        if (words.size() >= 2 && words.get(0).trim().equals("#")) {
            return words.get(1).trim();
        }
        return null;
    }

    private static String postBBody(String tag, String content) {
        return "{\"tag_name\":\"" + tag + "\",\"target_commitish\":\"master\"," +
                "\"body\":\"" + content + ",\"draft\":false,\"prerelease\":false" +
                ",\"generate_release_notes\":true}";
    }

    // For testing purpose
    public static void main(String[] args) {
        System.out.println(postBBody("0.10.20", "- Blablabla\n- Other thing..."));
        System.out.println(extractReleaseNote("0.10.19"));
        System.out.println("--------------");
    }

}
