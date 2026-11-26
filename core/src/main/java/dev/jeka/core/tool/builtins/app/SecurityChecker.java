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
import java.util.Locale;

class SecurityChecker {

    static final String GIT_TRUSTED_URL_PREFIX_PROP = "jeka.app.url.trusted";

    private static final String APPROVED_URL_PROP = "github.com/jeka-dev/";

    private static final String URL_SEPARATOR = " ";

    private static final String DOT_GIT = ".git";

    static final Path GLOBAL_PROP_FILE = JkLocator.getGlobalPropertiesFile();

    static boolean isAllowed(String gitUrl) {
        if (isTrustedBy(gitUrl, APPROVED_URL_PROP)) {
            return true;
        }
        JkProperties props = JkProperties.ofFile(GLOBAL_PROP_FILE);
        String approvedUrlString = props.get(GIT_TRUSTED_URL_PREFIX_PROP);
        if (JkUtilsString.isBlank(approvedUrlString)) {
            return false;
        }
        return Arrays.stream(approvedUrlString.split(URL_SEPARATOR))
                .anyMatch(trusted -> isTrustedBy(gitUrl, trusted));
    }

    /*
     * A trusted entry matches whole path segments: 'github.com/foo' matches 'github.com/foo/bar' but
     * not 'github.com/foo-bar/baz'. An entry ending with '.git' designates a single repository.
     */
    // non-private for testing
    static boolean isTrustedBy(String gitUrl, String trustedEntry) {
        String url = parseGitUrl(gitUrl);
        String entry = parseGitUrl(trustedEntry);
        while (entry.endsWith("/")) {
            entry = entry.substring(0, entry.length() - 1);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (entry.isEmpty() || isSuspicious(url)) {
            return false;
        }
        if (withoutDotGit(url).equals(withoutDotGit(entry))) {
            return true;
        }
        return !entry.endsWith(DOT_GIT) && url.startsWith(entry + "/");
    }

    // Such urls can be resolved by Git to another location than the one they start with.
    private static boolean isSuspicious(String url) {
        if (url.contains("%") || url.contains("\\") || url.contains("//") || url.matches("(?s).*[\\s\\p{Cntrl}].*")) {
            return true;
        }
        return Arrays.stream(url.split("/")).anyMatch(segment -> segment.equals(".") || segment.equals(".."));
    }

    private static String withoutDotGit(String url) {
        return url.endsWith(DOT_GIT) ? url.substring(0, url.length() - DOT_GIT.length()) : url;
    }

    static void addTrustedUrl(String allowedUrl) {
        String sanitized = parseGitUrl(allowedUrl);
        PropFile propFile = new PropFile(GLOBAL_PROP_FILE);
        propFile.appendValueToMultiValuedProp(GIT_TRUSTED_URL_PREFIX_PROP, sanitized, URL_SEPARATOR);
    }

    /*
     * Returns the url as [host]/[path], without protocol, user info and tag.
     */
    // non-private for testing
    static String parseGitUrl(String gitUrl) {
        String url = JkUtilsString.nullToEmpty(gitUrl).trim();
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex >= 0) {
            url = url.substring(0, fragmentIndex);
        }
        int protocolIndex = url.indexOf("://");
        if (protocolIndex >= 0) {
            url = url.substring(protocolIndex + 3);
        }
        int slashIndex = url.indexOf('/');
        String authority = slashIndex < 0 ? url : url.substring(0, slashIndex);
        String path = slashIndex < 0 ? "" : url.substring(slashIndex);
        authority = authority.substring(authority.lastIndexOf('@') + 1);

        // 'host:owner/repo' notation. The owner must be kept. A port number is part of the host.
        int colonIndex = authority.indexOf(':');
        if (colonIndex >= 0 && !authority.substring(colonIndex + 1).matches("\\d+")) {
            path = "/" + authority.substring(colonIndex + 1) + path;
            path = path.startsWith("//") ? path.substring(1) : path;
            authority = authority.substring(0, colonIndex);
        }
        return authority.toLowerCase(Locale.ROOT) + path;
    }
}
