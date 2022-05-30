package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JkSearchTest {

    @Test
    @Ignore
    public void testSearchWithSpecifiedUrl() throws IOException {
        JkLog.setDecorator(JkLog.Style.BRACE);
        List result = JkModuleSearch.of()
                .setApiUrl("https://oss.sonatype.org/service/local/lucene/search")
                        .setGroupOrNameCriteria("guav")
                        .search();
        result.forEach(System.out::println);
    }

    @Test
    @Ignore
    public void testSearchWithOssrh() throws IOException {
        JkLog.setDecorator(JkLog.Style.BRACE);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepo repo = JkRepo.ofMavenOssrhDownloadAndDeploySnapshot("djeang", System.getenv("jiraPwd"));
        List result = JkModuleSearch.of(repo)
                .setGroupOrNameCriteria("guav")
                .search();
        result.forEach(System.out::println);
        JkModuleSearch.of(repo)
                .setGroupOrNameCriteria("guav")
                .search();
        JkLog.setVerbosity(verbosity);
    }

    @Test
    @Ignore
    public void testSearchWithMavenCentral() throws IOException {
        JkLog.setDecorator(JkLog.Style.BRACE);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepo repo = JkRepo.ofMavenCentral();
        List result = JkModuleSearch.of(repo)
                .setGroupOrNameCriteria("guav")
                .search();
        result.forEach(System.out::println);
        JkLog.setVerbosity(verbosity);
    }

    @Test
    @Ignore
    public void testSearchWithMavenCentralWithVersion() throws IOException {
        JkLog.setDecorator(JkLog.Style.BRACE);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepo repo = JkRepo.ofMavenCentral();
        List result = JkModuleSearch.of(repo)
                .setGroupOrNameCriteria("org.springframework.boot:")
                .search();
        result.forEach(System.out::println);
        JkLog.setVerbosity(verbosity);
    }

}