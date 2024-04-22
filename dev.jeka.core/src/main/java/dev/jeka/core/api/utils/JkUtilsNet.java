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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.system.JkLog;

import java.io.*;
import java.net.*;

public class JkUtilsNet {

    public static void assertResponseOk(HttpURLConnection con, String body) throws IOException {
        int code = con.getResponseCode();
        if (code >= 400) {
            InputStream inputStream = con.getErrorStream();
            if (inputStream == null) {
                throw new IllegalStateException("Request " + con.getRequestMethod() + " " + con.getURL()
                        + " failed with status code " + code + "\nRequest body : " + body);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new IllegalStateException("Request " + con.getURL() + " failed with status code " + code + "\n"
                        + "Request body : " + body + "\n"
                        + "Response body : " + response);
            }
        }
    }

    /**
     * Weak implementation of heath checker, but maybe good enough for certain scenario.
     */
    public static void checkUntilOk(String url, int timeoutMillis, int sleepMillis) {
        long start = System.currentTimeMillis();
        int statusCode = -1;

        while ( sinceStart(start) < timeoutMillis ) {
            try {
                URL actualUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) actualUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                statusCode = connection.getResponseCode();
                JkLog.debug("Pinging %s returned %s%n", url, statusCode);
                if (isOK(statusCode)) {
                    JkLog.verbose("Check ping on %s returned success code : %s", url, statusCode);
                    return;
                }
                JkLog.debug("Check ping on %s returned fail code : %s. Waiting %s ms before next attemp", url,
                        statusCode, sleepMillis);
                connection.disconnect();
                JkUtilsSystem.sleep(sleepMillis);
            } catch (Exception e) {
                JkLog.debug("Error pinging %s : %s. Sleep %s millis before next attempt", url ,e.getMessage(),
                        sleepMillis);
                JkUtilsSystem.sleep(sleepMillis);
            }
        }
        throw new IllegalStateException("Request " + url + " failed with status code " + statusCode +
                " for more than " + timeoutMillis +  " millis.");

    }

    public static boolean isStatusOk(String url, boolean log) {
        try {
            URL actualUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) actualUrl.openConnection();
            connection.setRequestMethod("GET");
            if (log) {
                System.out.printf("Pinging %s ...", url);
            }
            connection.connect();
            int statusCode = connection.getResponseCode();
            if (log) {
                System.out.printf(" result status %s%n", statusCode);
            }
            return isOK(statusCode);
        } catch (ConnectException e) {
           return false;  // can't connect may mean no server listen
        } catch (ProtocolException | MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Checks if a port is open on a specified host.
     *
     * @param hostAndPort A string representing the host and port in the format "host:port"
     * @param millis      The timeout value in milliseconds
     */
    public static boolean checkPortOpen(String hostAndPort, int millis) {
        String[] hostPort = hostAndPort.split(":");
        int portNum = Integer.parseInt(hostPort[1]);
        return checkPortOpen(hostPort[0], portNum, millis);
    }

    /**
     * Checks if a port is open on a specified host.
     */
    public static boolean checkPortOpen(String host, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return true; // If connect() succeeds, the port is open
        } catch (IOException e) {
            return false; // If connect() fails, the port is closed
        }
    }

    /**
     * Finds a free port within the specified range.
     */
    public static int findFreePort(int from, int to) {
        for (int port = from; port <= to; port++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                JkLog.verbose("Tested ort %s is already in use", port);
            }
        }
        throw new IllegalStateException("No free port found between " + from + " and " + to);
    }

    /**
     * Sends an HTTP request to the specified URL using the specified method and parameters.
     *
     * @param url    The URL to send the request to
     * @param method The HTTP method to use (e.g., "GET", "POST", etc.)
     * @param params The parameters to include in the request body
     * @return The response from the server as a BasicHttpResponse object
     * @throws UncheckedIOException If an I/O error occurs while sending the request or receiving the response
     */
    public static BasicHttpResponse sendHttpRequest(String url, String method, String params) {
        try {
            URL obj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setDoOutput(params != null);

            // Send POST request
            if (params != null) {
                OutputStream os = httpURLConnection.getOutputStream();
                os.write(params.getBytes());
                os.flush();
                os.close();
            }

            // Get Response
            int responseCode = httpURLConnection.getResponseCode();
/*
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    httpURLConnection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
*/
            return new BasicHttpResponse(responseCode, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public static class BasicHttpResponse {

        public final int code;

        public final String body;

        public BasicHttpResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public boolean isOk() {
            return code < 400;
        }

        public void asserOk() {
            JkUtilsAssert.state(isOk(), "Returned HTTP status code was not OK : %s", code);
        }
    }

    private static boolean isOK(int statusCode) {
        return statusCode >= 200 && statusCode < 400;
    }

    private static long sinceStart(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
