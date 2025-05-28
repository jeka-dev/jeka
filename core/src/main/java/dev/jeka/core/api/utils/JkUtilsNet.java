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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);

                try {
                    statusCode = connection.getResponseCode();
                } catch (SocketTimeoutException e) {
                    statusCode = -1;
                }
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
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            if (log) {
                System.out.printf("Pinging %s ...%n", url);
            }
            Thread.yield();
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
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean isAvailableAndOk(String url, boolean log) {
        try {
            return isStatusOk(url, log);
        } catch (RuntimeException e) {
            JkLog.debug("Error while pinging %s : %s", url, e.getMessage());
            return false;
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
     * @deprecated Use {@link dev.jeka.core.api.http.JkHttpRequest#execute()} instead
     */
    @Deprecated
    public static BasicHttpResponse sendHttpRequest(String url, String method, Map<String, String> headers, String requestBody) {
        try {
            URL obj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
            httpURLConnection.setRequestMethod(method);
            headers.forEach(httpURLConnection::setRequestProperty);
            httpURLConnection.setDoOutput(requestBody != null);

            // Send POST request
            if (requestBody != null) {
                OutputStream os = httpURLConnection.getOutputStream();
                os.write(requestBody.getBytes());
                os.flush();
                os.close();
            }

            // Get Response
            int responseCode = httpURLConnection.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    httpURLConnection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return new BasicHttpResponse(responseCode, response.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    /**
     * @deprecated Use {@link dev.jeka.core.api.http.JkHttpRequest#execute()} instead
     */
    @Deprecated
    public static BasicHttpResponse sendHttpRequest(String url, String method, String requestBody) {
        return sendHttpRequest(url, method, new HashMap<>(), requestBody);
    }

    /**
     * @deprecated Use {@link dev.jeka.core.api.http.JkHttpRequest#downloadFile(Path)} instead
     */
    @Deprecated
    public static void downloadFile(String fileURL, Path saveFilePath) {
        URL url = JkUtilsIO.toUrl(fileURL);
        downloadFile(url, saveFilePath, httpURLConnection -> {});
    }

    /**
     * @deprecated Use {@link dev.jeka.core.api.http.JkHttpRequest#downloadFile(Path)} instead
     */
    @Deprecated
    public static void downloadFile(URL url, Path saveFilePath,
                                    Consumer<HttpURLConnection> urlConnectionCustomizer) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setInstanceFollowRedirects(true);
            urlConnectionCustomizer.accept(connection);
            if (JkLog.isDebug()) {
                String authHeader = connection.getHeaderField("Authorization");
                JkLog.debug("Download file " + url);
                if (authHeader != null) {
                    String[] authHeaderValues = authHeader.split(" ");
                    JkLog.debug("User Authorization header: " + authHeaderValues[0] + " xxxxx");
                }
            }
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                JkUtilsPath.createDirectories(saveFilePath.getParent());
                FileOutputStream outputStream = new FileOutputStream(saveFilePath.toString());
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
            } else {
                connection.disconnect();
                throw new IllegalStateException("No file to download at " + url
                        + " : Server replied with HTTP code: " + responseCode);
            }
            connection.disconnect();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    /**
     * @deprecated Use {@link dev.jeka.core.api.http.JkHttpResponse} instead
     */
    @Deprecated
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
