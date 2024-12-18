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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

public class JkGitTest {

    JkGit git = JkGit.of(Paths.get(""));

    @Test
    public void getCurrentBranch() {
        System.out.printf("--%s--%n", git.getCurrentBranch());
    }

    @Test
    @Ignore  // May fail on githubAction build when doing a release (tag ??)
    public void isSyncWithRemote() {
        System.out.printf("--%s--%n", git.isSyncWithRemote());
    }

    @Test
    public void isWorkspaceDirty() {
        System.out.printf("--%s--%n", git.isWorkspaceDirty());
    }

    @Test
    public void getCurrentCommit() {
        System.out.printf("--%s--%n", git.getCurrentCommit());
    }

    @Test
    public void getTagsOnCurrentCommit() {
        System.out.printf("--%s--%n", git.getTagsOnCurrentCommit());
    }

    @Test
    public void diff() {
       JkGit.FileList fileList = git.diiff("22453db1627c559c2c3549a1c54793697366332c",
                "250f6702d7bf9ac1b12881f5e6384393f2b76cdc");
        Assert.assertEquals(1, fileList.get().size());
       fileList.get().forEach(System.out::println);
       Assert.assertTrue(fileList.hasFileStartingWith("docs/"));
    }

    @Test
    public void badCommand_throwException() {
        Exception thrown = null;
        try {
            JkGit.of().execCmdLine("bad command");
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNotNull(thrown);
    }


}