package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * For tests relying on Maven Central: they are skipped instead of failing when Maven Central does not answer,
 * as it happens on CI from time to time.
 */
public final class MavenCentralTestSupport {

    private static final String PROBE_URL = JkRepo.MAVEN_CENTRAL_URL + "/dev/jeka/jeka-core/maven-metadata.xml";

    private static final int TIMEOUT_MILLIS = 10_000;

    private MavenCentralTestSupport() {
    }

    /**
     * Runs the specified test code. If it fails, the test is skipped when Maven Central does not answer, and
     * fails as usual otherwise.
     */
    public static void runOrSkipIfCentralDown(Executable testCode) throws Throwable {
        runOrSkipIfDown(PROBE_URL, testCode);
    }

    static void runOrSkipIfDown(String probeUrl, Executable testCode) throws Throwable {
        try {
            testCode.execute();
        } catch (Throwable failure) {
            String probeError = probe(probeUrl);
            if (probeError != null) {
                Assumptions.abort("Maven repository does not answer (" + probeError + "). Test failure was: " + failure);
            }
            throw failure;
        }
    }

    // Returns null if Maven Central answers, or the reason why it does not
    static String probe(String probeUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(probeUrl).toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            try {
                int code = connection.getResponseCode();
                return code == HttpURLConnection.HTTP_OK ? null : "HTTP " + code + " on " + probeUrl;
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            return e + " on " + probeUrl;
        }
    }

}
