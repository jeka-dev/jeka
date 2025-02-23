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

package dev.jeka.core.tool.builtins.tooling.git;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocUrl;
import dev.jeka.core.tool.KBean;

@JkDoc("Provides convenient operations for Git.")
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-git/")
public final class GitKBean extends KBean {

    public final JkGit git = JkGit.of(getBaseDir());

    @JkDoc("Performs a dirty check first, put a tag at the HEAD and push it to the remote repo." +
            " The user will be prompted to enter the tag name.")
    public void tagRemote() {
        git.tagRemote();
    }

    @JkDoc("Displays last git tag in current branch")
    public void lastTag() {
        System.out.println(JkGit.of(getBaseDir()).setLogWithJekaDecorator(false).getLatestTag());
    }

    @JkDoc("Displays all commit messages since last tag")
    public void lastCommitMessages() {
        JkGit.of(getBaseDir()).setLogWithJekaDecorator(false)
                .getCommitMessagesSinceLastTag().forEach(msg -> System.out.println("- " + msg));
    }

    @JkDoc("Moves the 'latest' tag to the current commit and push it to origin.")
    public void moveLatest() {
        moveTagOnCurrentCommit("latest");
    }

    private void moveTagOnCurrentCommit(String tagName) {
        boolean found = git.copy().setCollectStdout(true).addParams("tag").exec()
                .getStdoutAsMultiline().stream()
                    .anyMatch(line -> line.equals(tagName));
        if (found) {
            git.execCmd("push", "origin", "--delete", tagName);
            git.copy().execCmd("tag", "-d", tagName);
        }
        git.execCmd("tag", "-f", tagName);
        git.execCmd("push", "origin", tagName, "-f");
    }

    /**
     * Gets the current version either from commit message if specified nor from git tag.
     */
    public String version() {
        return gerVersionFromGit().getVersion();
    }

    /**
     * Returns a configured {@link JkVersionFromGit}.
     */
    public JkVersionFromGit gerVersionFromGit() {
        return JkVersionFromGit.of(this.getBaseDir(), "");
    }

}
