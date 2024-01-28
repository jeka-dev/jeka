package dev.jeka.core.api.utils;

import dev.jeka.core.api.system.JkLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JkUtilsHttp {

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
        int statusCode = 0;
        long start = System.currentTimeMillis();

        while ((statusCode < 200 || statusCode >= 400) || (System.currentTimeMillis() - start) < timeoutMillis) {
            try {
                URL actualUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) actualUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                statusCode = connection.getResponseCode();
                JkLog.info("Pinging % returned %s", url, statusCode);

                // sleep for 2 seconds before the next request
                if (statusCode < 200 || statusCode >= 400) {
                    JkUtilsSystem.sleep(sleepMillis);
                }
                connection.disconnect();
            } catch (Exception e) {
                JkLog.warn("Error pinging " + url + " :" + e.getMessage());
                JkUtilsSystem.sleep(sleepMillis);
            }
        }

    }
}
