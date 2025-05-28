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

package dev.jeka.plugins.centralportal;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.JkPropValue;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import java.nio.file.Path;
import java.time.LocalDateTime;

@JkDoc("Plugin for publishing artifacts to Maven Central.\n" +
        "Run `jeka centralportal:publish` to publish artifacts.")
public class CentralportalKBean extends KBean {

    @JkPropValue("JEKA_CENTRAL_PORTAL_USERNAME")
    @JkDoc("The token user name to connect to Central Portal")
    public String username;

    @JkPropValue("JEKA_CENTRAL_PORTAL_PASSWORD")
    @JkDoc("The token password to connect to Central Portal")
    public String password;

    @JkPropValue("JEKA_CENTRAL_PORTAL_SIGN_KEY")
    @JkDoc("The armored GPG key to sign published artifacts.")
    public String signingKey;

    @JkPropValue("JEKA_CENTRAL_PORTAL_SIGN_KEY_PASSPHRASE")
    @JkDoc("The passphrase of the armored GPG key.")
    public String signingKeyPassphrase;

    @JkDoc("If true, the bundle will be automatically deployed to Maven Central without manual intervention.")
    public boolean automatic = true;

    @JkDoc("Wait time in seconds for successful publication")
    public int timeout = 1000 * 60 * 15;

    private String deploymentId;

    @JkPostInit(required = true)
    private void postInit(MavenKBean mavenKBean) {}

    @JkDoc("Publishes artifacts to Maven Central.")
    public void publish() {

        MavenKBean mavenKBean = this.load(MavenKBean.class);
        JkMavenPublication publication = mavenKBean.getPublication();
        if (publication.getVersion().isSnapshot()) {
            JkLog.verbose("Current version %s is SNAPSHOT: won't publish to Central Portal", publication.getVersion());
            return;
        }
        JkLog.startTask("Publishing artifacts to Central Portal");
        Path tempDir = getOutputDir().resolve("portalcentral");
        JkFileSigner signer = JkGpgSigner.ofAsciiKey(signingKey, signingKeyPassphrase);
        JkCentralPortalBundler bundleMaker = JkCentralPortalBundler.of(tempDir, signer);
        Path zipPath = getOutputDir().resolve("centralportal-bundle-" + publication.getModuleId().getName()
                + "-" + publication.getVersion() + ".zip");
        bundleMaker.bundle(publication, zipPath);

        JkCentralPortalPublisher publisher = JkCentralPortalPublisher.of(username, password);
        deploymentId = publisher.publish(zipPath, automatic);
        JkLog.info("Bundle " + zipPath + " uploaded on Central Portal server, deploymentId: " + deploymentId);

        JkLog.info("Waiting for validation. Start waiting at %s.", LocalDateTime.now());
        publisher.waitUntilValidate(deploymentId, timeout);
        JkLog.info("Deployment validated successfully.");

        if (automatic) {
            JkLog.info("Waiting for publication. Start waiting at %s.", LocalDateTime.now());
            publisher.waitUntilPublishing(deploymentId, timeout);
            JkLog.info("Deployment publication successful.");
        }
        JkLog.endTask();
    }

    /**
     * Retrieves the deployment ID resulting from the publishing process to Maven Central.
     * This method should be invoked after the "publish" method has been invoked.
     *
     * @return the deployment ID as a string, which uniquely identifies the specific deployment.
     */
    public String getDeploymentId() {
        return deploymentId;
    }

}
