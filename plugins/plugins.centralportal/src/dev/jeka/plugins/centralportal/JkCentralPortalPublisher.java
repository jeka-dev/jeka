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

import dev.jeka.core.api.marshalling.json.JkJson;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.http.JkHttpResponse;
import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides functionality for managing deployment bundles on Maven Central repositories (e.g., Sonatype Central).
 * Handles upload operations and monitors deployment status through REST API interactions.
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Upload deployment bundles to Maven Central</li>
 *   <li>Monitor deployment status using unique identifiers</li>
 *   <li>Track deployment state transitions</li>
 *   <li>Support both automatic and manual publishing workflows</li>
 * </ul>
 *
 * <h3>Common Operations:</h3>
 * <ol>
 *   <li>Bundle upload and initial deployment</li>
 *   <li>Status verification through REST endpoints</li>
 *   <li>Deployment state monitoring with configurable timeouts</li>
 *   <li>Publication process management (automatic/manual)</li>
 * </ol>
 *
 * <p>This class communicates with central repository REST APIs using HTTP protocols
 * for all deployment and status management operations.</p>
 */
public class JkCentralPortalPublisher {

    private static final String UPLOAD_URL = "https://central.sonatype.com/api/v1/publisher/upload";

    private static final String STATUS_URL = "https://central.sonatype.com/api/v1/publisher/status";

    public static final String VALIDATED_STATUS = "VALIDATED";

    public static final String PUBLISHING_STATUS = "PUBLISHING";

    public static final String PUBLISHED_STATUS = "PUBLISHED";

    public static final String FAILED_STATUS = "FAILED";

    private static final List<String> STATUSES = Arrays.asList(
            "PENDING",
            "VALIDATING",
            VALIDATED_STATUS,
            PUBLISHING_STATUS,
            PUBLISHED_STATUS,
            FAILED_STATUS);

    private static final List<String> PUBLISH_SUCCESS_STATUS = Arrays.asList(PUBLISHING_STATUS, PUBLISHED_STATUS);

    private final String username;

    private final String password;

    private JkCentralPortalPublisher(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Creates a new instance of {@code JkCentralPortalPublisher} using the provided username and password.
     *
     * @param username the username used for authenticating with the central portal
     * @param password the password associated with the given username
     * @return a new instance of {@code JkCentralPortalPublisher}
     */
    public static JkCentralPortalPublisher of(String username, String password) {
        return new JkCentralPortalPublisher(username, password);
    }

    /**
     * @param automatic if <code>true</code>, the bundle will be automatically released to Maven central, without
     *                  needing manual intervention.
     * @return the DeploymentId
     */
    String publish(Path bundle, boolean automatic) {
        try {
            return doPublish(bundle, automatic);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Retrieves the deployment status for the specified deployment ID.
     * This method wraps the retrieval logic and converts any {@code IOException} into an unchecked exception.
     *
     * @param deploymentId the unique identifier of the deployment whose status is to be retrieved
     * @return the current status of the deployment associated with the provided deployment ID
     * @throws UncheckedIOException if an I/O error occurs during the status retrieval process
     */
    public StatusResponse getStatus(String deploymentId) {
        try {
            return doGetStatus(deploymentId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Waits until the specified deployment has reached the "validated" status or one of the "publish success" statuses
     * within the provided timeout duration.
     *
     * @param deploymentId   the unique identifier of the deployment to monitor
     * @param timeoutSeconds the maximum duration, in seconds, to wait for the deployment to reach a valid status
     */
    public void waitUntilValidate(String deploymentId, int timeoutSeconds) {
        List<String> successStatuses = new ArrayList<>(PUBLISH_SUCCESS_STATUS);
        successStatuses.add(VALIDATED_STATUS);
        waitUntil(deploymentId, timeoutSeconds, 5, successStatuses);
    }

    /**
     * Waits for the deployment with the specified deployment ID to reach the publishing phase
     * until it achieves a "publish success" status within the provided timeout duration.
     *
     * @param deploymentId   the unique identifier of the deployment to monitor
     * @param timeoutSeconds the maximum duration, in seconds, to wait for the deployment to reach the publishing state
     */
    public void waitUntilPublishing(String deploymentId, int timeoutSeconds) {
        waitUntil(deploymentId, timeoutSeconds, 7, PUBLISH_SUCCESS_STATUS);
    }

    private void waitUntil(String deploymentId, int timeoutSecond, int elapseRetrySecond, List<String> statuses) {
        try {
            doWaitUntil(deploymentId, timeoutSecond, elapseRetrySecond, statuses);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void doWaitUntil(String deploymentId, int timeoutSecond, int elapseRetrySecond, List<String> statuses)
            throws IOException {

        long start = System.currentTimeMillis();
        StatusResponse status = null;
        while (System.currentTimeMillis() - start < timeoutSecond * 1000L) {
            status = getStatus(deploymentId);
            JkLog.verbose("Current Status: %s", status);
            if (FAILED_STATUS.equals(status.deploymentState)) {
                throw new IllegalStateException("Deployment " + deploymentId + " has status FAILED: "
                        + status.errorList());
            }
            if (statuses.contains(status.deploymentState)) {
                return;
            }
            JkUtilsSystem.sleep(elapseRetrySecond * 1000L);
        }
        throw new IllegalStateException("Timed out of " + timeoutSecond + "s waiting for deployment status of "
                + deploymentId + ". " + "Last status was: " + status);
    }

    private StatusResponse doGetStatus(String deploymentId) throws IOException {
        String url = STATUS_URL + "?id=" + deploymentId;

        JkHttpResponse response = JkHttpRequest.of(url, "POST")
                .useCaches(false)
                .addHeader("Authorization", authorizationHeader())
                .execute();

        if (response.isError()) {
            final String msg = String.format("Error getting status of deploymentId: %s, status code: %s," +
                                " message: %s", deploymentId, response.getStatusCode(), response.getBody());
            throw new IllegalStateException(msg);
        }

        // Read the response
        StatusResponse statusResponse = JkJson.of().parse(response.getBody(), StatusResponse.class);
        JkLog.verbose("Deployment status for %s: %s ", deploymentId, statusResponse.deploymentState);
        return statusResponse;
    }

    private String doPublish(Path bundle, boolean automatic) throws IOException {

        String url = UPLOAD_URL + (automatic ? "?publishingType=AUTOMATIC" : "");

        JkHttpResponse response = JkHttpRequest.ofMultipart(url)
                .addHeader("Authorization", authorizationHeader())
                .addMultipartFile("bundle", bundle)
                .execute();

        if (response.isError()) {
            final String msg = String.format("Error publishing bundle. on: %s responded status code: %s, message: %s",
                    UPLOAD_URL, response.getStatusCode(), response.getBody());
            throw new IllegalStateException(msg);
        }

        // The body is supposed to contain the deploymentId
        return response.getBody();
    }

    private String authorizationHeader() {
        String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return "Bearer " + encoded;
    }

    public static class StatusResponse {

        public String deploymentId;

        public String deploymentName;

        public String deploymentState;

        public List<String> purls = new ArrayList<>();

        public Map<String, Object> errors = new HashMap<>();

        @Override
        public String toString() {
            return JkJson.of().toJson(this);
        }

        public List<String> errorList() {
            return errors.values().stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        }
    }
}
