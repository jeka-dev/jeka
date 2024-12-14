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
import dev.jeka.core.tool.KBean;

@JkDoc("Provides project versioning by extracting Git information" + "\n" +
        "The version is inferred from git using following logic : "+ "\n" +
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT"+ "\n" +
        "  - If last commit contains a message containing [commentKeyword]xxxxx, version values xxxxx"+ "\n" +
        "  - If last commit is tagged, version values [last tag on last commit]"+ "\n" +
        "The inferred version can be applied to project.publication.maven.version and project.publication.ivy.publication, " +
                "programmatically using 'handleVersioning' method."
)
public final class GitKBean extends KBean {

    public final JkGit git = JkGit.of(getBaseDir());

    @JkDoc("Performs a dirty check first, put a tag at the HEAD and push it to the remote repo." +
            " The user will be prompted to enter the tag name.")
    public void tagRemote() {
        git.tagRemote();
    }

    @JkDoc("Displays version supplied to the project.")
    public void showVersion() {
        JkLog.info(gerVersionFromGit().getVersion());
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
