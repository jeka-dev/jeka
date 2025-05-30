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
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A utility class for constructing and executing HTTP requests using {@code HttpURLConnection}.
 * This class provides methods for configuring various aspects of the HTTP request such as headers,
 * body, connection timeouts, and more. It also supports multipart file uploads.
 */
public class JkHttpRequest {

    private final HttpURLConnection connection;

    private JkHttpRequest(HttpURLConnection connection) {
        this.connection = connection;
    }

    /**
     * Creates an instance of {@code HttpRequest} configured with the specified URL and HTTP method.
     *
     * @param url    the URL for the HTTP request.
     * @param method the HTTP method to be used for the request (e.g., "GET", "POST").
     * @return a new {@code HttpRequest} object representing the HTTP request.
     * @throws UncheckedIOException if an I/O error occurs while creating the connection.
     */
    public static JkHttpRequest of(String url, String method) {
        try {
            URL urlConn = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlConn.openConnection();
            conn.setRequestMethod(method.toUpperCase());
            return new JkHttpRequest(conn);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates an instance of {@code HttpRequest} configured with the specified URL
     * and defaults to the "GET" HTTP method.
     *
     * @param url the URL for the HTTP request.
     * @return a new {@code HttpRequest} object representing the HTTP request.
     * @throws UncheckedIOException if an I/O error occurs while creating the connection.
     */
    public static JkHttpRequest of(String url) {
        return of(url, "GET");
    }

    /**
     * Creates an instance of {@code HttpRequest} configured for a multipart/form-data POST request.
     *
     * @param url the URL for the HTTP request.
     * @return a new {@code HttpRequest} object configured for multipart/form-data handling.
     * @throws UncheckedIOException if an I/O error occurs while creating the connection.
     */
    public static JkHttpRequest ofMultipart(String url) {
        return of(url, "POST")
                .addHeader("Content-Type", "multipart/form-data; boundary=" + MultipartPart.MULTIPART_BOUNDARY)
                .useCaches(false)
                .setDoInput(true)
                .setDoOutput(true);
    }

    /**
     * Sets the HTTP request method for this {@code HttpRequest}.
     *
     * @param method the HTTP method to be used for the request (e.g., "GET", "POST").
     *               The provided method will be converted to upper case.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     * @throws UncheckedIOException if the specified method is invalid or an I/O error occurs.
     */
    public JkHttpRequest setRequestMethod(String method) {
        try {
            connection.setRequestMethod(method.toUpperCase());
        } catch (ProtocolException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Adds a header to the HTTP request.
     *
     * @param name  the name of the header to be added.
     * @param value the value of the header to be added.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     */
    public JkHttpRequest addHeader(String name, String value) {
        connection.setRequestProperty(name, value);
        return this;
    }

    public JkHttpRequest addHeaders(Map<String, String> headers) {
        headers.forEach(this::addHeader);
        return this;
    }

    /**
     * Adds a multipart part to the HTTP request. This method writes the content of the
     * given {@code MultipartPart} to the underlying {@link HttpURLConnection}'s output stream.
     *
     * @param part the {@link MultipartPart} to be added to the HTTP request. The part
     *             must contain the necessary field name, file name, and file content to
     *             be included in the multipart request body.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     * @throws UncheckedIOException if an I/O error occurs while flushing the part's content to the stream.
     */
    public JkHttpRequest addMultipartPart(MultipartPart part) {
        try {
            part.doFlush(connection.getOutputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Adds a file as a multipart part to the HTTP request. The file is included in the
     * multipart request body with the specified field name and file name.
     *
     * @param fieldName the field name for the file in the multipart request.
     * @param fileName  the name of the file to be sent in the multipart request.
     * @param file      the {@link Path} representing the file to be added.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     */
    public JkHttpRequest addMultipartFile(String fieldName, String fileName, Path file) {
        return addMultipartPart(MultipartPart.ofFile(fieldName, fileName, file));
    }

    /**
     * Adds a file as a multipart part to the HTTP request. The file is included in the
     * multipart request body with the specified field name and the file's name derived from
     * the provided {@link Path}.
     *
     * @param fieldName the field name for the file in the multipart request.
     * @param file      the {@link Path} representing the file to be added.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     */
    public JkHttpRequest addMultipartFile(String fieldName, Path file) {
        return addMultipartFile(fieldName, file.getFileName().toString(), file);
    }

    /**
     * Sets the body content for this {@code HttpRequest}. The provided body is written to the
     * output stream of the underlying {@link HttpURLConnection}.
     * The method enables output mode on the connection and ensures the body content is sent
     * in the request.
     *
     * @param body the content to be used as the request body. The content is encoded as bytes
     *             using the platform's default charset.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     * @throws UncheckedIOException if an I/O error occurs while writing the body content to
     *                              the output stream.
     */
    public JkHttpRequest setBody(String body) {
        connection.setDoOutput(true);
        try {
            OutputStream os = connection.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Configures whether the underlying {@link HttpURLConnection} should use caches.
     *
     * @param useCaches a boolean indicating whether caching is enabled for this connection.
     *                  If {@code true}, the connection is allowed to use cached responses.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     */
    public JkHttpRequest useCaches(boolean useCaches) {
        connection.setUseCaches(useCaches);
        return this;
    }

    /**
     * Sets the read timeout for the HTTP request.
     * The read timeout is the amount of time, in milliseconds,
     * to wait for a response from the server after the request has been sent.
     *
     * @param readTimeout the timeout value in milliseconds. A value of 0 means no timeout.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     */
    public JkHttpRequest setReadTimeout(int readTimeout) {
        connection.setReadTimeout(readTimeout);
        return this;
    }

    public JkHttpRequest setFollowRedirect(boolean followRedirect) {
        connection.setInstanceFollowRedirects(followRedirect);
        return this;
    }

    public JkHttpRequest setConnectionTimeout(int connectionTimeout) {
        connection.setConnectTimeout(connectionTimeout);
        return this;
    }

    public JkHttpRequest setDoInput(boolean doInput) {
        connection.setDoInput(doInput);
        return this;
    }

    public JkHttpRequest setDoOutput(boolean doOutput) {
        connection.setDoOutput(doOutput);
        return this;
    }

    /**
     * Applies a customizer function to the underlying {@link HttpURLConnection}.
     * This allows for direct customization of the {@link HttpURLConnection} instance
     * associated with this {@code HttpRequest}.
     *
     * @param customizer a {@link Consumer} that accepts the {@link HttpURLConnection} to be customized.
     *                   The provided consumer can perform operations such as setting headers,
     *                   configuring connection properties, or manipulating the connection.
     * @return this {@code HttpRequest} instance, allowing for method chaining.
     */
    public JkHttpRequest customize(Consumer<HttpURLConnection> customizer) {
        customizer.accept(connection);
        return this;
    }

    /**
     * Executes the HTTP request configured for this {@code HttpRequest} instance
     * and returns the corresponding {@code HttpResponse}.
     *
     * @return an {@code HttpResponse} object containing the status code, headers,
     * and body of the response received from the server.
     */
    public JkHttpResponse execute() {
        JkLog.debug("%s %s", connection.getRequestMethod(), connection.getURL());
        JkHttpResponse response = JkHttpResponse.get(connection);
        connection.disconnect();
        return response;
    }

    /**
     * Consumes the response stream of an HTTP request, providing the status code and
     * input stream to a specified {@link BiConsumer}. The method attempts to retrieve
     * the response code and input stream from the underlying HTTP connection, and then
     * applies the provided consumer to process them. The HTTP connection is disconnected
     * after processing.
     *
     * @param consumer a {@link BiConsumer} that accepts the HTTP response status code
     *                 and the input stream representing the response content. The consumer
     *                 is responsible for handling the provided data.
     * @return the HTTP response status code received from the server.
     * @throws UncheckedIOException if an I/O error occurs while accessing the connection
     *                              or input stream.
     */
    public int consumeResponseStream(BiConsumer<Integer, InputStream> consumer) {
        int statusCode;
        try {
            statusCode = connection.getResponseCode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (statusCode > HttpURLConnection.HTTP_BAD_REQUEST) {
            JkLog.debug("HTTP response status code for %s: %d", connection.getURL(), statusCode);
            try {
                JkLog.debug("HTTP response status msg: %s", JkHttpResponse.readResponseBody(connection), statusCode);
            } catch (IOException e) {
                JkLog.debug("HTTP response status msg not available");
            }
        }
        try {
            consumer.accept(statusCode, connection.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            connection.disconnect();
        }
        return statusCode;
    }

    /**
     * Downloads a file from the HTTP server and saves it to the specified file path.
     *
     * @param downloadFilePath the path where the downloaded file will be saved. The parent
     *                         directories are created if they do not exist.
     * @return the HTTP response status code from the server after the download attempt.
     * @throws IllegalStateException if the server responds with a status code indicating
     *                               an error (HTTP 4xx or 5xx) or if there is no file to
     *                               download at the specified URL.
     */
    public int downloadFile(Path downloadFilePath) {
        BiConsumer<Integer, InputStream> consumer = (statusCode, inputStream) -> {
            if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new IllegalStateException("No file to download at " + connection.getURL()
                        + " : Server replied with statud code: " + statusCode);
            }
            JkUtilsPath.createDirectories(downloadFilePath.getParent());
            JkUtilsPath.copy(inputStream, downloadFilePath, StandardCopyOption.REPLACE_EXISTING);
        };
        return consumeResponseStream(consumer);
    }

    public static class MultipartPart {

        private static final String MULTIPART_BOUNDARY = "----WebKitFormBoundary" + UUID.randomUUID();

        private static final String LINE_FEED = "\r\n";

        private final String fieldName;

        private final String fileName;

        private final Path file;

        private MultipartPart(String fieldName, String fileName, Path file) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.file = file;
        }

        public static MultipartPart ofFile(String fieldName, String fileName, Path file) {
            return new MultipartPart(fieldName, fileName, file);
        }

        private void flush(OutputStream os) {
            try {
                doFlush(os);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void doFlush(OutputStream output) throws IOException {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
            writer.append("--").append(MULTIPART_BOUNDARY).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"");
            if (fileName != null) {
                writer.append("; filename=\"").append(fileName).append("\"");
            }
            writer.append(LINE_FEED);
            if (file != null) {
                writer.append("Content-Type: ").append(Files.probeContentType(file)).append(LINE_FEED);
            }
            writer.append(LINE_FEED).flush();

            if (file != null) {
                try (InputStream inputStream = Files.newInputStream(file)) {
                    JkUtilsIO.copy(JkUtilsIO.inputStream(file.toFile()), output);
                }
            }
            writer.append(LINE_FEED).flush();

            // --- End multipart/form-data
            writer.append("--").append(MULTIPART_BOUNDARY).append("--").append(LINE_FEED);
            writer.close();
        }
    }
}
