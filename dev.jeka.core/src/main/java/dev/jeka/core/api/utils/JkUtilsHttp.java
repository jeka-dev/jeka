package dev.jeka.core.api.utils;

import java.io.*;
import java.net.*;

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
        long start = System.currentTimeMillis();

        while ( sinceStart(start) < timeoutMillis ) {
            try {
                URL actualUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) actualUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int statusCode = connection.getResponseCode();
                System.out.printf("Pinging %s returned %s%n", url, statusCode);
                if (isOK(statusCode)) {
                    break;
                }
                connection.disconnect();
                JkUtilsSystem.sleep(sleepMillis);
            } catch (Exception e) {
                System.out.println("Error pinging " + url + " :" + e.getMessage());
                JkUtilsSystem.sleep(sleepMillis);
            }
        }

    }

    public static boolean isStatusOk(String url) {
        try {
            URL actualUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) actualUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int statusCode = connection.getResponseCode();
            System.out.printf("Pinging %s returned %s%n", url, statusCode);
            return isOK(statusCode);
        } catch (ConnectException e) {
           return false;  // can't connect may mean no server listen
        } catch (ProtocolException | MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isOK(int statusCode) {
        return statusCode >= 200 && statusCode < 400;
    }

    private static long sinceStart(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
