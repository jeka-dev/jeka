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

package dev.jeka.core.api.tooling.git;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JkGitTest {

    JkGit git = JkGit.of(Paths.get(""));

    @Test
    void getCurrentBranch() {
        System.out.printf("--%s--%n", git.getCurrentBranch());
    }

    @Test
    @Disabled  // May fail on githubAction build when doing a release (tag ??)
    void isSyncWithRemote() {
        System.out.printf("--%s--%n", git.isSyncWithRemote());
    }

    @Test
    void isWorkspaceDirty() {
        System.out.printf("--%s--%n", git.isWorkspaceDirty());
    }

    @Test
    void getCurrentCommit() {
        System.out.printf("--%s--%n", git.getCurrentCommit());
    }

    @Test
    void getTagsOnCurrentCommit() {
        System.out.printf("--%s--%n", git.getTagsOnCurrentCommit());
    }

    @Test
    void diff() {
       JkGit.FileList fileList = git.diiff("22453db1627c559c2c3549a1c54793697366332c",
                "250f6702d7bf9ac1b12881f5e6384393f2b76cdc");
       Assertions.assertEquals(1, fileList.get().size());
       fileList.get().forEach(System.out::println);
       Assertions.assertTrue(fileList.hasFileStartingWith("docs/"));
    }

    @Test
    void badCommand_throwException() {
        Exception thrown = null;
        try {
            JkGit.of().execCmdLine("bad command");
        } catch (Exception e) {
            thrown = e;
        }
        Assertions.assertNotNull(thrown);
    }

    @Test
    void testGetRemoteTag() {

        // Test with null tag
        String jekaRepo = "https://github.com/jeka-dev/jeka.git";
        String currentCommit = JkGit.of().getRemoteTagCommit(jekaRepo, null);
        assertFalse(currentCommit.contains(" "));
        assertFalse(currentCommit.contains("\t"));
        assertFalse(currentCommit.contains("HEAD"));

        // test with existing tag
        System.out.println();
        currentCommit = JkGit.of().getRemoteTagCommit(jekaRepo, "0.11.11");
        assertFalse(currentCommit.contains(" "));
        assertFalse(currentCommit.contains("\t"));
        assertFalse(currentCommit.contains("HEAD"));
    }

    @Test
    @Disabled
    void testGetRemoteTag_noTags_returnsEmpty() {
        // This repo may have tags in the future, that's why test is disabled
        String taglessRepo = "https://github.com/jeka-dev/demo-build-templates-consumer.git";
        List<JkGit.Tag> tags =  JkGit.of().getRemoteTags(taglessRepo);
        assertTrue(tags.isEmpty());
    }

    @Test
    void testGitTagParseCmdLineResponse() {
        String cliResponse = "c7bff170c6d2657bef3198d6ee3cb3856728dca1        refs/tags/0.11.0-beta.7";
        JkGit.Tag gitTag = JkGit.Tag.ofGitCmdlineResult(cliResponse);
        System.out.println(gitTag);
        assertEquals("c7bff170c6d2657bef3198d6ee3cb3856728dca1", gitTag.getCommitHash());
        assertEquals("0.11.0-beta.7", gitTag.getName());
    }

    @Test
    void testGetRemoteTagCommit() {
        String branch = JkGit.of().getRemoteDefaultBranch("https://github.com/jeka-dev/jeka");
        assertNotNull(branch);
        System.out.println(branch);
    }

}