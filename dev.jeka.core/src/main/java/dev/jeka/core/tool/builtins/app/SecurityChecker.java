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

package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class SecurityChecker {

    static final String GIT_URLS_APPROVED_PROP = "jeka.apps.url.approved";

    private static final String APPROVED_URL_PROP = "github.com/jeka-dev/";

    private static final String URL_SEPARATOR = ", ";

    static final Path GLOBAL_PROP_FILE = JkLocator.getGlobalPropertiesFile();

    static boolean isAllowed(String gitUrl) {
        String sanitized = parseGitUrl(gitUrl).trim();
        if (sanitized.startsWith(APPROVED_URL_PROP)) {
            return true;
        }
        JkProperties props = JkProperties.ofFile(GLOBAL_PROP_FILE);
        String approvedUrlString = props.get(GIT_URLS_APPROVED_PROP);
        if (JkUtilsString.isBlank(approvedUrlString)) {
            return false;
        }
        List<String> approvedUrls = Arrays.asList(approvedUrlString.split(URL_SEPARATOR));
        return approvedUrls.stream()
                .map(String::trim)
                .anyMatch(sanitized::startsWith);
    }

    static void addAllowedUrl(String allowedUrl) {
        String sanitized = parseGitUrl(allowedUrl).trim();
         new PropFile(GLOBAL_PROP_FILE).appendValueToMultiValuedProp(GIT_URLS_APPROVED_PROP,
                 sanitized, URL_SEPARATOR, 120);
    }

    // non-private for testing
    static String parseGitUrl(String gitUrl) {

        // http/https
        if (gitUrl.startsWith("https://") || gitUrl.startsWith("http://")) {
            String afterProtocol = JkUtilsString.substringAfterFirst(gitUrl, "//");
            String firstSegment = JkUtilsString.substringBeforeFirst(afterProtocol, "/");
            if (firstSegment.contains("@")) {
                return JkUtilsString.substringAfterFirst(afterProtocol, "@");
            }
            return afterProtocol;
        }
        // ssl
        String afterProtocol = gitUrl.replace("ssh://", "");
        afterProtocol = afterProtocol.replace("git@", "");
        String firstSegment = JkUtilsString.substringBeforeFirst(afterProtocol, "/");
        if (firstSegment.contains(":")) {
            String userPart = ":" + JkUtilsString.substringAfterFirst(firstSegment, ":");
            return afterProtocol.replace(userPart, "");
        }
        return afterProtocol;
    }
}
