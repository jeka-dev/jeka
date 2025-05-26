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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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

    private static final String LINE_FEED = "\r\n";

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
    public String getStatus(String deploymentId) {
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

    private void doWaitUntil(String deploymentId, int timeoutSecond, int elapseRetrySecond, List<String> statuses) throws IOException {
        long start = System.currentTimeMillis();
        String status = null;
        while (System.currentTimeMillis() - start < timeoutSecond * 1000L) {
            status = getStatus(deploymentId);
            if (FAILED_STATUS.equals(status)) {
                throw new IllegalStateException("Deployment " + deploymentId + " has status FAILED.");
            }
            JkLog.verbose("Deployment status for %s: %s ", deploymentId, status);
            if (statuses.contains(status)) {
                return;
            }
            JkUtilsSystem.sleep(elapseRetrySecond * 1000L);
        }
        throw new IllegalStateException("Timed out of " + timeoutSecond + "s waiting for deployment status of "
                + deploymentId + ". " + "Last status was: " + status);
    }

    private String doGetStatus(String deploymentId) throws IOException {
        URL obj = new URL(STATUS_URL + "?id=" + deploymentId);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);

        String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", "Bearer " + encoded);

        // Get the response
        int responseCode = conn.getResponseCode();

        if (responseCode >= 400) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                final String response = br.lines().reduce("", String::concat);
                final String msg = String.format("Error getting status of deploymentId: %s, status code: %s," +
                                " message: %s", deploymentId, responseCode, response);
                throw new IllegalStateException(msg);
            }
        }

        // Read the response
        String response;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            response = br.lines().reduce("", String::concat);
            List<String> tokens = Arrays.asList(response.split(":")).stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i).replace('"', ' ').trim();
                if (token.equals("deploymentState")) {
                    conn.disconnect();
                    return tokens.get(i + 1).replace('"', ' ').trim();
                }

            }
        }
        conn.disconnect();
        throw new IllegalStateException("No deployment status found for deploymentId: " + deploymentId);
    }

    private String doPublish(Path bundle, boolean automatic) throws IOException {

        URL obj = new URL(UPLOAD_URL + (automatic ? "?publishingType=AUTOMATIC" : ""));
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);

        String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", "Bearer " + encoded);

        String boundary = "===" + System.currentTimeMillis() + "===";
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            // Start the form
            writer.append("Content-Disposition: form-data; name=\"bundle\"; filename=\"central-bundle.zip\"");
            writer.append(LINE_FEED);
            writer.append("Content-Type: application/zip");
            writer.append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary");
            writer.append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            // Copy the file
            Files.copy(bundle, os);
            os.flush();

            // End of multipart/form-data
            writer.append(LINE_FEED);

            writer.append("--").append(boundary).append("--");
            writer.append(LINE_FEED);
        }

        // Get the response
        int responseCode = conn.getResponseCode();

        if (responseCode >= 400) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                final String response = br.lines().reduce("", String::concat);
                final String msg = String.format("Error publishing bundle on: %s, status code: %s, message: %s", UPLOAD_URL,
                        responseCode, response);
                throw new IllegalStateException(msg);
            }
        }

        // Read the response
        String response;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            response = br.readLine();
        }
        conn.disconnect();
        return response;
    }
}
