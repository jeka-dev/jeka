package dev.jeka.core.api.depmanagement;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.BasicURLHandler;
import org.apache.ivy.util.url.IvyAuthenticator;

/**
 * BasicURLHandler does not handle http redirect. This class fixes this missing
 * feature.
 */
class IvyFollowRedirectUrlHandler extends BasicURLHandler {

    private static final int BUFFER_SIZE = 64 * 1024;

    @Override
    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        // Install the IvyAuthenticator
        if ("http".equals(src.getProtocol()) || "https".equals(src.getProtocol())) {
            IvyAuthenticator.install();
        }

        URLConnection srcConn = null;
        try {
            src = normalizeToURL(src);
            srcConn = src.openConnection();
            srcConn.setRequestProperty("User-Agent", getUserAgent());
            srcConn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            if (srcConn instanceof HttpURLConnection) {
                final HttpURLConnection httpCon = (HttpURLConnection) srcConn;
                final boolean redirect = checkRedirect(httpCon);
                if (redirect) {
                    final String newUrl = httpCon.getHeaderField("Location");
                    disconnect(srcConn);
                    download(new URL(newUrl), dest, l);
                    return;
                }
                if (!checkStatusCode(src, httpCon)) {
                    throw new IOException("The HTTP response code for " + src
                            + " did not indicate a success." + " See log for more detail.");
                }
            }

            // do the download
            final InputStream inStream = getDecodingInputStream(srcConn.getContentEncoding(),
                    srcConn.getInputStream());
            FileUtil.copy(inStream, dest, l);

            // check content length only if content was not encoded
            if (srcConn.getContentEncoding() == null) {
                final int contentLength = srcConn.getContentLength();
                if (contentLength != -1 && dest.length() != contentLength) {
                    dest.delete();
                    throw new IOException(
                            "Downloaded file size doesn't match expected Content Length for " + src
                                    + ". Please retry.");
                }
            }

            // update modification date
            final long lastModified = srcConn.getLastModified();
            if (lastModified > 0) {
                dest.setLastModified(lastModified);
            }
        } finally {
            disconnect(srcConn);
        }
    }

    private boolean checkRedirect(HttpURLConnection con) throws IOException {
        final int status = con.getResponseCode();
        return status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER;
    }

    private boolean checkStatusCode(URL url, HttpURLConnection con) throws IOException {
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

    private void disconnect(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            if (!"HEAD".equals(((HttpURLConnection) con).getRequestMethod())) {
                // We must read the response body before disconnecting!
                // Cfr.
                // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
                // [quote]Do not abandon a connection by ignoring the response
                // body. Doing
                // so may results in idle TCP connections.[/quote]
                readResponseBody((HttpURLConnection) con);
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
    private void readResponseBody(HttpURLConnection conn) {
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

    private static final class HttpStatus {
        static final int SC_OK = 200;

        static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;

        private HttpStatus() {
        }
    }

}
