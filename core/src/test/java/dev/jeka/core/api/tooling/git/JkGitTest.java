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

import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.project.JkProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JkGitTest {

    @Test
    void version_onDetachedHead(@TempDir Path dir) throws Exception {
        for (String args : new String[] {"init -q", "config user.email a@b.c", "config user.name a",
                "config commit.gpgsign false", "config tag.gpgsign false"}) {
            git(dir, args);
        }
        Path file = dir.resolve("file.txt");
        Files.write(file, "a".getBytes());
        git(dir, "add .");
        git(dir, "commit -q -m first");
        git(dir, "checkout -q --detach");
        JkGit detachedGit = JkGit.of(dir);
        String expected = detachedGit.getCurrentCommit();

        // No tag
        String version = detachedGit.getVersionFromTag();
        assertTrue(version.endsWith("-SNAPSHOT"));
        String hash = version.replace("-SNAPSHOT", "");
        assertTrue(hash.length() >= 4 && expected.startsWith(hash), version);
        assertEquals(version, JkVersionFromGit.of(dir, "").getVersion());
        assertEquals(version, detachedGit.getVersionFromCommitMessage("Release:"));

        // Tag
        git(dir, "tag 1.2.3");
        assertEquals("1.2.3", detachedGit.getVersionFromTag());
        assertEquals("1.2.3", JkVersionFromGit.of(dir, "").getVersion());

        // Dirty
        Files.write(file, "b".getBytes());
        assertEquals(version, JkVersionFromGit.of(dir, "").getVersion());

        // No branch in manifest
        JkProject project = JkProject.of().setBaseDir(dir);
        JkVersionFromGit.of(dir, "").handleVersioning(project);
        JkManifest manifest = project.packaging.getManifest();
        assertNotNull(manifest.getMainAttribute("Git-commit"));
        assertFalse(manifest.asString().contains("Git-branch"), manifest.asString());
    }

    // 'git ls-remote' lists an annotated tag twice: 'X' with the tag object, and 'X^{}' with the commit
    @Test
    void getRemoteTags_annotatedTag_listedOnceWithItsCommit(@TempDir Path dir) throws Exception {
        for (String args : new String[] {"init -q", "config user.email a@b.c", "config user.name a",
                "config commit.gpgsign false", "config tag.gpgsign false"}) {
            git(dir, args);
        }
        Files.write(dir.resolve("file.txt"), "a".getBytes());
        git(dir, "add .");
        git(dir, "commit -q -m first");
        git(dir, "tag 1.0.0");
        git(dir, "tag -a 1.1.0 -m annotated");
        String commit = JkGit.of(dir).getCurrentCommit();
        String repoUrl = dir.toUri().toString();

        List<JkGit.Tag> tags = JkGit.of().getRemoteTags(repoUrl);

        assertEquals(List.of("1.0.0", "1.1.0"), JkGit.of().getRemoteTagAsStrings(repoUrl));
        tags.forEach(tag -> assertEquals(commit, tag.getCommitHash(), tag.getName()));
        assertEquals(commit, JkGit.of().getRemoteTagCommit(repoUrl, "1.1.0"));
        assertEquals(commit, JkGit.of().getRemoteTagCommit(repoUrl, "1.0.0"));
    }

    @Test
    void getLatestTag_noTag_returnsNull(@TempDir Path dir) throws Exception {
        for (String args : new String[] {"init -q", "config user.email a@b.c", "config user.name a",
                "config commit.gpgsign false", "config tag.gpgsign false"}) {
            git(dir, args);
        }
        Files.write(dir.resolve("file.txt"), "a".getBytes());
        git(dir, "add .");
        git(dir, "commit -q -m first");
        JkGit jkGit = JkGit.of(dir);

        assertNull(jkGit.getLatestTag());
        git(dir, "tag 1.0");
        assertEquals("1.0", jkGit.getLatestTag());
    }

    @Test
    void versionFromTag_detachedHead_honorsPrefix(@TempDir Path dir) throws Exception {
        initRepoWithOneCommit(dir);
        git(dir, "tag other");
        git(dir, "checkout -q --detach");
        JkGit detachedGit = JkGit.of(dir);

        // A tag without the prefix does not give the version
        assertTrue(detachedGit.getVersionFromTag("v").endsWith("-SNAPSHOT"));

        git(dir, "tag v1.0");
        assertEquals("1.0", detachedGit.getVersionFromTag("v"));
        assertEquals("1.0", JkVersionFromGit.of(dir, "v").getVersion());
    }

    @Test
    void versionFromTag_branchWithSlash_replacedByDash(@TempDir Path dir) throws Exception {
        initRepoWithOneCommit(dir);
        git(dir, "checkout -q -b feat/slash");

        assertEquals("feat-slash-SNAPSHOT", JkGit.of(dir).getVersionFromTag());
        assertEquals("feat-slash-SNAPSHOT", JkGit.of(dir).getVersionFromCommitMessage("Release:"));
    }

    private static void initRepoWithOneCommit(Path dir) throws Exception {
        for (String args : new String[] {"init -q", "config user.email a@b.c", "config user.name a",
                "config commit.gpgsign false", "config tag.gpgsign false"}) {
            git(dir, args);
        }
        Files.write(dir.resolve("file.txt"), "a".getBytes());
        git(dir, "add .");
        git(dir, "commit -q -m first");
    }

    @Test
    void version_noCommit_explicitError(@TempDir Path dir) throws Exception {
        git(dir, "init -q");
        Exception e = assertThrows(IllegalStateException.class, () -> JkVersionFromGit.of(dir, "").getVersion());
        assertTrue(e.getMessage().contains("at least one commit"), e.getMessage());
    }

    private static void git(Path dir, String args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args.split(" ")));
        assertEquals(0, new ProcessBuilder(command).directory(dir.toFile()).inheritIO().start().waitFor());
    }

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