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

import org.junit.jupiter.api.Test;

import static dev.jeka.core.tool.builtins.app.SecurityChecker.isTrustedBy;
import static dev.jeka.core.tool.builtins.app.SecurityChecker.parseGitUrl;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityCheckerTest {

    @Test
    public void testParseGitUrl() {
        assertEquals("github.com/jeka/Jeka/", parseGitUrl("https://github.com/jeka/Jeka/"));
        assertEquals("github.com/user/repo.git", parseGitUrl("https://username:token@github.com/user/repo.git"));
        assertEquals("github.com/user/repo.git", parseGitUrl("git@github.com:user/repo.git"));
        assertEquals("github.com/user/repo.git", parseGitUrl("ssh://git@github.com:user/repo.git"));
        assertEquals("github.com/user/repo.git", parseGitUrl("ssh://git@github.com/user/repo.git"));
        assertEquals("github.com/user/repo", parseGitUrl("https://GitHub.com/user/repo#1.0"));
        assertEquals("host:8443/user/repo", parseGitUrl("https://host:8443/user/repo"));
    }

    @Test
    public void isTrustedBy_wholeSegments() {
        assertTrue(isTrustedBy("https://github.com/djeang/repo", "github.com/djeang/"));
        assertTrue(isTrustedBy("https://github.com/djeang/repo", "github.com/djeang"));
        assertTrue(isTrustedBy("git@github.com:djeang/repo.git", "github.com/djeang/"));
        assertTrue(isTrustedBy("https://github.com/djeang/repo.git#1.0", "github.com/djeang/repo"));
        assertTrue(isTrustedBy("https://github.com/djeang/repo", "github.com/djeang/repo.git"));
        assertFalse(isTrustedBy("https://github.com/djeang-other/repo", "github.com/djeang"));
        assertFalse(isTrustedBy("https://github.com/djeang/repo-other", "github.com/djeang/repo"));
        assertFalse(isTrustedBy("https://github.com/djeang/repo", ""));
        assertFalse(isTrustedBy("https://github.com/djeang/repo", "/"));
    }

    @Test
    public void isTrustedBy_ownerIsKeptInSshUrl() {
        assertFalse(isTrustedBy("git@github.com:other/repo.git", "github.com/djeang/repo.git"));
        assertFalse(isTrustedBy("git@github.com:other/djeang", "github.com/djeang"));
        assertFalse(isTrustedBy("git@github.com:other/repo.git", "github.com/repo.git"));  // recorded by former versions
        assertFalse(isTrustedBy("https://github.com/repo/any", "github.com/repo.git"));
    }

    @Test
    public void isTrustedBy_suspiciousUrls() {
        String trusted = "github.com/jeka-dev/";
        assertTrue(isTrustedBy("https://github.com/jeka-dev/demo", trusted));
        assertFalse(isTrustedBy("https://github.com/jeka-dev/../other/repo", trusted));
        assertFalse(isTrustedBy("https://github.com/jeka-dev/./../other/repo", trusted));
        assertFalse(isTrustedBy("https://github.com/jeka-dev/%2e%2e/other/repo", trusted));
        assertFalse(isTrustedBy("https://github.com/jeka-dev/..\\other/repo", trusted));
        assertFalse(isTrustedBy("https://github.com/jeka-dev//other", trusted));
        assertFalse(isTrustedBy("https://github.com@other.com/jeka-dev/repo", trusted));
        assertFalse(isTrustedBy("https://other.com/github.com/jeka-dev/repo", trusted));
        assertFalse(isTrustedBy("https://github.com:8443/jeka-dev/repo", trusted));
    }
}
