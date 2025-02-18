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

package dev.jeka.plugins.nexus;

import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import java.util.function.Consumer;

@JkDoc("Releases Nexus repositories (as OSSRH) after publication")
public class NexusKBean extends KBean {

    @JkDoc("Comma separated filters for taking in account only repositories with specified profile names.")
    public String profileNamesFilter = "";

    @JkDoc("Timeout in seconds, before the 'close' operation times out.")
    public int closeTimeout = JkNexusRepos.DEFAULT_CLOSE_TIMEOUT_SECONDS;

    private final JkConsumers<JkNexusRepos> nexusReposConfigurators = JkConsumers.of();

    @JkDoc("Wraps Maven publish repo with Nexus autoclose trigger")
    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(mavenPublication -> {
            JkNexusRepos nexusRepos = getJkNexusRepos(mavenPublication);
            nexusRepos.setCloseTimeout(closeTimeout);
            nexusRepos.autoReleaseAfterPublication(mavenPublication);
        });
    }

    @JkDoc("Closes and releases the nexus repositories used by project KBean to publish artifacts.")
    public void closeAndRelease() {
        MavenKBean mavenKBean = getRunbase().find(MavenKBean.class).orElse(null);
        if (mavenKBean == null) {
            JkLog.error("No MavenKBean found in runbase %s.", getBaseDir());
            return;
        }
        JkNexusRepos nexusRepos  = getJkNexusRepos(mavenKBean.getMavenPublication());
        if (nexusRepos == null) {
            return;
        }
        nexusRepos.closeAndRelease();
    }

    /**
     * Adds a JkNexusRepos consumer that will be executed just in time.
     */
    public NexusKBean configureNexusRepo(Consumer<JkNexusRepos> nexusReposConfigurator) {
        this.nexusReposConfigurators.append(nexusReposConfigurator);
        return this;
    }

    private String[] profiles() {
        return JkUtilsString.isBlank(profileNamesFilter) ? new String[0] : profileNamesFilter.split(",");
    }

    private JkNexusRepos getJkNexusRepos(JkMavenPublication mavenPublication) {
        JkNexusRepos result = JkNexusRepos.ofPublishRepo(mavenPublication)
                .setProfileNameFilters(profiles());
        this.nexusReposConfigurators.accept(result);
        return result;
    }

}
