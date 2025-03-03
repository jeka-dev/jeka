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

package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.BasicURLHandler;
import org.apache.ivy.util.url.IvyAuthenticator;
import org.apache.ivy.util.url.TimeoutConstrainedURLHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/*
 * This implementation copy most of the code from its super class has this was not designed to be
 * friendly inherited.
 * The purpose is to :
 *   - add http headers to requests based upon the headers declared in JkRepo objects.
 *   - handle follow-redirect gracefully for download method
 */
final class IvyCustomUrlHandler extends BasicURLHandler {

    private static final class HttpStatus {
        static final int SC_OK = 200;

        static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;

        private HttpStatus() {
        }
    }

    private static final int BUFFER_SIZE = 64 * 1024;

    private final Map<String, Map<String, String>> urlHeaders;

    private IvyCustomUrlHandler(Map<String, Map<String, String>> urlHeaders) {
        this.urlHeaders = urlHeaders;
    }

    static IvyCustomUrlHandler of(JkRepoSet repoSet) {
        Map<String, Map<String, String>> urlHeaders = new HashMap<>();
        for (JkRepo repo : repoSet.getRepos()) {
            if (!JkUtilsIO.isHttpOrHttps(repo.getUrl())) {
                continue;
            }
            urlHeaders.put(repo.getUrl().toString(), repo.getHttpHeaders());
        }
        return new IvyCustomUrlHandler(urlHeaders);
    }

    @Override
    public void download(final URL src, final File dest, final CopyProgressListener listener,
                         final TimeoutConstraint timeoutConstraint) throws IOException {

        // Install the IvyAuthenticator
        if ("http".equals(src.getProtocol()) || "https".equals(src.getProtocol())) {
            IvyAuthenticator.install();
        }
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();

        URLConnection srcConn = null;
        try {
            final URL normalizedURL = normalizeToURL(src);
            srcConn = normalizedURL.openConnection();
            srcConn.setConnectTimeout(connectionTimeout);
            srcConn.setReadTimeout(readTimeout);
            srcConn.setRequestProperty("User-Agent", getUserAgent());
            srcConn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            if (srcConn instanceof HttpURLConnection) {
                addHeaders(srcConn);
                HttpURLConnection httpCon = (HttpURLConnection) srcConn;

                // Added By Jeka team : handle redirect gracefully
                final boolean redirect = checkRedirect(httpCon);
                if (redirect) {
                    final String newUrl = httpCon.getHeaderField("Location");
                    disconnectQuietly(srcConn);
                    download(new URL(newUrl), dest, listener, timeoutConstraint);
                    return;
                }
                // end of added block

                if (!checkStatusCode(normalizedURL, httpCon)) {
                    throw new IOException("The HTTP response code for " + normalizedURL
                            + " did not indicate a success." + " See log for more detail.");
                }
            }

            // do the download
            InputStream inStream = getDecodingInputStream(srcConn.getContentEncoding(),
                    srcConn.getInputStream());
            FileUtil.copy(inStream, dest, listener);

            // check content length only if content was not encoded
            if (srcConn.getContentEncoding() == null) {
                final int contentLength = srcConn.getContentLength();
                final long destFileSize = dest.length();
                if (contentLength != -1 && destFileSize != contentLength) {
                    dest.delete();
                    throw new IOException(
                            "Downloaded file size (" + destFileSize + ") doesn't match expected " +
                                    "Content Length (" + contentLength + ") for " + normalizedURL + ". Please retry.");
                }
            }

            // update modification date
            long lastModified = srcConn.getLastModified();
            if (lastModified > 0) {
                dest.setLastModified(lastModified);
            }
        } finally {
            disconnect(srcConn);
        }
    }

    @Override
    public InputStream openStream(final URL url, final TimeoutConstraint timeoutConstraint) throws IOException {
        // Install the IvyAuthenticator
        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
            IvyAuthenticator.install();
        }
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();

        URLConnection conn = null;
        try {
            final URL normalizedURL = normalizeToURL(url);
            conn = normalizedURL.openConnection();
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", getUserAgent());
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            if (conn instanceof HttpURLConnection) {
                addHeaders(conn);
                HttpURLConnection httpCon = (HttpURLConnection) conn;
                if (!checkStatusCode(normalizedURL, httpCon)) {
                    throw new IOException("The HTTP response code for " + normalizedURL
                            + " did not indicate a success." + " See log for more detail.");
                }
            }
            InputStream inStream = getDecodingInputStream(conn.getContentEncoding(),
                    conn.getInputStream());
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
            return new ByteArrayInputStream(outStream.toByteArray());
        } finally {
            disconnect(conn);
        }
    }

    @Override
    public void upload(final File src, final URL dest, final CopyProgressListener listener,
                       final TimeoutConstraint timeoutConstraint) throws IOException {

        if (!"http".equals(dest.getProtocol()) && !"https".equals(dest.getProtocol())) {
            throw new UnsupportedOperationException(
                    "URL repository only support HTTP PUT at the moment");
        }

        // Install the IvyAuthenticator
        IvyAuthenticator.install();

        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        HttpURLConnection conn = null;
        try {
            final URL normalizedDestURL = normalizeToURL(dest);
            conn = (HttpURLConnection) normalizedDestURL.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectionTimeout);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("User-Agent", getUserAgent());
            conn.setRequestProperty("Content-type", "application/octet-stream");
            conn.setRequestProperty("Content-length", Long.toString(src.length()));
            addHeaders(conn);
            conn.setInstanceFollowRedirects(true);

            try (final InputStream in = new FileInputStream(src)) {
                final OutputStream os = conn.getOutputStream();
                FileUtil.copy(in, os, listener);
            }


            validatePutStatusCode(normalizedDestURL, conn.getResponseCode(), conn.getResponseMessage());
        } finally {
            disconnect(conn);
        }

    }

    @Override
    public URLInfo getURLInfo(URL url, TimeoutConstraint timeoutConstraint) {
        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
            IvyAuthenticator.install();
        }
        final int connectionTimeout = (timeoutConstraint == null || timeoutConstraint.getConnectionTimeout() < 0) ? 0 : timeoutConstraint.getConnectionTimeout();
        final int readTimeout = (timeoutConstraint == null || timeoutConstraint.getReadTimeout() < 0) ? 0 : timeoutConstraint.getReadTimeout();
        URLConnection con = null;
        try {
            final URL normalizedURL = normalizeToURL(url);
            con = normalizedURL.openConnection();
            con.setConnectTimeout(connectionTimeout);
            con.setReadTimeout(readTimeout);
            con.setRequestProperty("User-Agent", getUserAgent());
            if (con instanceof HttpURLConnection) {
                HttpURLConnection httpCon = (HttpURLConnection) con;
                addHeaders(httpCon);
                if (getRequestMethod() == TimeoutConstrainedURLHandler.REQUEST_METHOD_HEAD) {
                    httpCon.setRequestMethod("HEAD");
                }
                if (checkStatusCode(normalizedURL, httpCon)) {
                    String bodyCharset = getCharSetFromContentType(con.getContentType());
                    return new MyUrlInfo(true, httpCon.getContentLength(), con.getLastModified(),
                            bodyCharset);
                }
            } else {
                int contentLength = con.getContentLength();
                if (contentLength <= 0) {
                    return UNAVAILABLE;
                } else {
                    String bodyCharset = getCharSetFromContentType(con.getContentType());
                    return new MyUrlInfo(true, contentLength, con.getLastModified(), bodyCharset);
                }
            }
        } catch (UnknownHostException e) {
            Message.warn("Host " + e.getMessage() + " not found. url=" + url);
            Message.info("You probably access the destination server through "
                    + "a proxy server that is not well configured.");
        } catch (IOException e) {
            Message.error("Server access error at url " + url, e);
        } finally {
            disconnect(con);
        }
        return UNAVAILABLE;
    }


    private void addHeaders(URLConnection urlConnection) {
        for (Map.Entry<String, Map<String, String>> entry : this.urlHeaders.entrySet()) {
            String fullUrl = urlConnection.getURL().toString();
            if (fullUrl.startsWith(entry.getKey())) {
                for (Map.Entry<String, String> headerEntry : entry.getValue().entrySet()) {
                    urlConnection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
                return;
            }
        }
    }

    private boolean checkRedirect(HttpURLConnection con) throws IOException {
        final int status = con.getResponseCode();
        return status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER;
    }

    private boolean checkReturnedStatusCode(URL url, HttpURLConnection con) throws IOException {
        final int status = con.getResponseCode();
        if (status == HttpStatus.SC_OK) {
            return true;
        }

        // IVY-1328: some servers return a 204 on a HEAD request
        if ("HEAD".equals(con.getRequestMethod()) && (status == 204)) {
            return true;
        }

        Message.debug("HTTP response status: " + status + " url=" + url);
        if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
            Message.warn("Your proxy requires authentication.");
        } else if (String.valueOf(status).startsWith("4")) {
            Message.verbose("CLIENT ERROR: " + con.getResponseMessage() + " url=" + url);
        } else if (String.valueOf(status).startsWith("5")) {
            Message.error("SERVER ERROR: " + con.getResponseMessage() + " url=" + url);
        }
        return false;
    }

    private void disconnectQuietly(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            if (!"HEAD".equals(((HttpURLConnection) con).getRequestMethod())) {
                // We must read the response body before disconnecting!
                // Cfr.
                // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
                // [quote]Do not abandon a connection by ignoring the response
                // body. Doing
                // so may results in idle TCP connections.[/quote]
                readResponseBodyQuietly((HttpURLConnection) con);
            }

            ((HttpURLConnection) con).disconnect();
        } else if (con != null) {
            try {
                con.getInputStream().close();
            } catch (final IOException e) {
                // ignored
            }
        }
    }

    /**
     * Read and ignore the response body.
     */
    private void readResponseBodyQuietly(HttpURLConnection conn) {
        final byte[] buffer = new byte[BUFFER_SIZE];

        InputStream inStream = null;
        try {
            inStream = conn.getInputStream();
            while (inStream.read(buffer) > 0) {
                // Skip content
            }
        } catch (final IOException e) {
            // ignore
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }

        final InputStream errStream = conn.getErrorStream();
        if (errStream != null) {
            try {
                while (errStream.read(buffer) > 0) {
                    // Skip content
                }
            } catch (final IOException e) {
                // ignore
            } finally {
                try {
                    errStream.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    private boolean checkStatusCode(URL url, HttpURLConnection con) throws IOException {
        int status = con.getResponseCode();
        if (status == HttpStatus.SC_OK) {
            return true;
        }

        // IVY-1328: some servers return a 204 on a HEAD request
        if ("HEAD".equals(con.getRequestMethod()) && (status == 204)) {
            return true;
        }

        Message.debug("HTTP response status: " + status + " url=" + url);
        if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
            Message.warn("Your proxy requires authentication.");
        } else if (String.valueOf(status).startsWith("4")) {
            Message.verbose("CLIENT ERROR: " + con.getResponseMessage() + " url=" + url);
        } else if (String.valueOf(status).startsWith("5")) {
            Message.error("SERVER ERROR: " + con.getResponseMessage() + " url=" + url);
        }
        return false;
    }

    private void disconnect(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            if (!"HEAD".equals(((HttpURLConnection) con).getRequestMethod())) {
                // We must read the response body before disconnecting!
                // Cfr. http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
                // [quote]Do not abandon a connection by ignoring the response body. Doing
                // so may results in idle TCP connections.[/quote]
                readResponseBody((HttpURLConnection) con);
            }

            ((HttpURLConnection) con).disconnect();
        } else if (con != null) {
            try {
                con.getInputStream().close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    private void readResponseBody(HttpURLConnection conn) {
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream inStream = conn.getInputStream()) {
            while (inStream.read(buffer) > 0) {
                // Skip content
            }
        } catch (IOException e) {
            // ignore
        }

        InputStream errStream = conn.getErrorStream();
        if (errStream != null) {
            try {
                while (errStream.read(buffer) > 0) {
                    // Skip content
                }
            } catch (IOException e) {
                // ignore
            } finally {
                try {
                    errStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    class MyUrlInfo extends URLInfo {

        protected MyUrlInfo(boolean available, long contentLength, long lastModified) {
            super(available, contentLength, lastModified);
        }

        protected MyUrlInfo(boolean available, long contentLength, long lastModified, String bodyCharset) {
            super(available, contentLength, lastModified, bodyCharset);
        }
    }



}
