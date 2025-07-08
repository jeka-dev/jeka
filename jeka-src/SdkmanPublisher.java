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

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.marshalling.json.JkJson;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.util.List;

class SdkmanPublisher {

    private static final long MAX_WAIT_MS = 120 * 60 * 1000;

    private static final long DELAY_MS = 60 * 1000;

    private static final String RELEASE_URL = "https://vendors.sdkman.io/release";

    private static final String DEFAULT_URL = "https://vendors.sdkman.io/default";

    private static final String CONSUMER_KEY = "???";

    private static final String TOKEN_ENV_VAR = "??????";

    private static final String CANDIDATE = "jeka";

    static void publish() {
        List<String> tags = JkGit.of().getTagsOnCurrentCommit();
        if (tags.isEmpty()) {
            JkLog.info("No tags on current commit");
            return;
        }
        String tag = tags.get(0);
        JkVersion version = JkVersion.of(tag);
        if (!version.isDigitsOnly() || version.toItems().size() != 3) {
            JkLog.info("Current tag %s is not a semVer version: won't publish.", tag);
            return;
        }
        String url = String.format("https://repo1.maven.org/maven2/dev/jeka/jeka-core/%s/jeka-core-%s-sdkman.zip",
                tag, tag);

        // Wait until the artifact is accessible from MavenCentral
        boolean retry = true;
        long start = System.currentTimeMillis();
        boolean found = false;
        while(retry) {
            JkUtilsSystem.sleep(DELAY_MS);
            found = !JkHttpRequest.of(url, "HEAD").execute().isError();
            long duration = JkUtilsTime.durationInMillis(start);
            retry = !found && duration < MAX_WAIT_MS;
        }
        if (!found) {
            JkLog.error("Artifact % not deployed after %min. SdkMan deployment aborted.", url, MAX_WAIT_MS/(1000*60));
            System.exit(1);
        }
        release(url, tag);
        setAsDefault(tag);
    }

    private static void release(String url, String version) {
        post(RELEASE_URL, url, version);
    }

    private static void setAsDefault(String version) {
        post(DEFAULT_URL, null, version);
    }

    private static void post(String sdkmanUrl, String candidateUrl, String version) {
        String token = System.getenv(TOKEN_ENV_VAR);
        String body = JkJson.of().toJson(new Body(version, candidateUrl));
        JkHttpRequest.of(sdkmanUrl, "POST")
                .addHeader("Consumer-Key", CONSUMER_KEY)
                .addHeader("Consumer-Token", token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .setBody(body)
                .execute()
                .assertNoError();
    }

    private static class Body {

        final String candidate = CANDIDATE;

        final String version;

        final String url;

        public Body(String version, String url) {
            this.version = version;
            this.url = url;
        }
    }
}
