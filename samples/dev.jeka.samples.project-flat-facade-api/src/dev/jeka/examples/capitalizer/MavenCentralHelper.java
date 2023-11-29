package dev.jeka.examples.capitalizer;

import com.github.djeang.vincerdom.VDocument;

import java.io.InputStream;
import java.net.URL;

public class MavenCentralHelper {

    public static String getLatestestJekaVersion() throws Exception {
        String url = "https://search.maven.org/solrsearch/select?q=g:%22dev.jeka%22+AND+a:%22jeka-core%22&rows=1&wt=xml";
        try (InputStream inputStream = new URL(url).openStream()){
            VDocument doc = VDocument.parse(inputStream);
            return doc.root().xPath("result/doc/str[@name='latestVersion']").get(0).text();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(getLatestestJekaVersion());
    }
}
