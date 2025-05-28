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

package dev.jeka.core.api.http;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class JkHttpResponse {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;

    public JkHttpResponse(int statusCode, Map<String, List<String>> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public static JkHttpResponse get(HttpURLConnection connection) {
        try {
            int code = connection.getResponseCode();
            String body = readResponseBody(connection);
            Map<String, List<String>> responseHeaders = getResponseHeaders(connection);
            return new JkHttpResponse(code, responseHeaders, body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isError() {
        return statusCode >= HttpURLConnection.HTTP_BAD_REQUEST;
    }

    public void assertNoError() {
        JkUtilsAssert.state(!isError(), "Returned HTTP status code was not OK : %s", this.statusCode);
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "{" +
                "statusCode=" + statusCode +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }

    static String readResponseBody(HttpURLConnection conn) throws IOException {
        InputStream inputStream;
        int status = conn.getResponseCode();

        // Use error stream for non-2xx responses
        if (status < HttpURLConnection.HTTP_BAD_REQUEST) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();  // may be null
        }

        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString().trim(); // remove final newline
        }
    }

    private static Map<String, List<String>> getResponseHeaders(HttpURLConnection conn) {
        try {
            // Trigger the actual request
            int statusCode = conn.getResponseCode();
            String statusMessage = conn.getResponseMessage();
            JkLog.verbose("HTTP Status: " + statusCode + " " + statusMessage);

            // Read headers
            Map<String, List<String>> headers = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String name = entry.getKey();
                List<String> values = entry.getValue();
                if (name != null) {
                    JkLog.verbose(name + ": " + java.lang.String.join(", ", values));
                }
            }
            return headers;

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read response headers", e);
        }
    }
}
