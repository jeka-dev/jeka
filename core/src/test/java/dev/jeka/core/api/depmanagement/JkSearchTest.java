package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class JkSearchTest {

    @Test
    @Ignore
    public void testSearchWithSpecifiedUrl() throws IOException {
        JkLog.setDecorator(JkLog.Style.INDENT);
        List result = JkCoordinateSearch.of()
                .setApiUrl("https://oss.sonatype.org/service/local/lucene/search")
                        .setGroupOrNameCriteria("guav")
                        .search();
        result.forEach(System.out::println);
    }

    @Test
    @Ignore
    public void testSearchWithOssrh() throws IOException {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepo repo = JkRepo.ofMavenOssrhDownloadAndDeploySnapshot("djeang", System.getenv("jiraPwd"));
        List result = JkCoordinateSearch.of(repo)
                .setGroupOrNameCriteria("guav")
                .search();
        result.forEach(System.out::println);
        JkCoordinateSearch.of(repo)
                .setGroupOrNameCriteria("guav")
                .search();
        JkLog.setVerbosity(verbosity);
    }

    @Test
    @Ignore
    public void testSearchWithMavenCentral() throws IOException {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepo repo = JkRepo.ofMavenCentral();
        List result = JkCoordinateSearch.of(repo)
                .setGroupOrNameCriteria("guav")
                .search();
        result.forEach(System.out::println);
        JkLog.setVerbosity(verbosity);
    }

    @Test
    @Ignore
    public void testSearchWithMavenCentralWithVersion() throws IOException {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkRepo repo = JkRepo.ofMavenCentral();
        List result = JkCoordinateSearch.of(repo)
                .setGroupOrNameCriteria("org.springframework.boot")
                .search();
        result.forEach(System.out::println);
        JkLog.setVerbosity(verbosity);
    }

    @Test
    @Ignore
    public void testSearchSpring() throws IOException {
        JkLog.setDecorator(JkLog.Style.INDENT);
        List result = JkCoordinateSearch.of(JkRepo.ofMavenCentral())
                .setGroupOrNameCriteria("org.sprin*")
                .search();
        result.forEach(System.out::println);
    }

    @Test
    @Ignore
    public void testSearchSpringpom()  {
        JkLog.setDecorator(JkLog.Style.INDENT);
        List result = JkCoordinateSearch.of(JkRepo.ofMavenCentral())
                .setGroupOrNameCriteria("org.springframework.boot:spring-boot-dependencies::")
                .search();
        result.forEach(System.out::println);
    }

    @Test
    @Ignore
    public void testSJekaVersions()  {
        JkLog.setDecorator(JkLog.Style.INDENT);
        List<String> result = JkCoordinateSearch.of(JkRepo.ofMavenCentral())
                .setTimeout(10000)
                .setGroupOrNameCriteria("dev.jeka:jeka-core:")
                .search();
        JkVersion max = result.stream()
                .map(s -> JkUtilsString.substringAfterLast(s, ":"))
                .map(JkVersion::of)
                .filter(JkVersion::isDigitsOnly)
                .max(Comparator.naturalOrder()).orElse(null);
        System.out.println(max);
    }


}