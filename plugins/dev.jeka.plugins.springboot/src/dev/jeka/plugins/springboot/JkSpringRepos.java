package dev.jeka.plugins.springboot;


import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;

/**
 * Download repositories of <i>Spring IO</i> company.
 */
public final class JkSpringRepos {

    public static final JkRepo SNAPSHOT = JkRepo.of("https://repo.spring.io/snapshot/");

    public static final JkRepo MILESTONE = JkRepo.of("https://repo.spring.io/milestone/");

    public static final JkRepo RELEASE = JkRepo.of("https://repo.spring.io/release/");

    public static JkRepoSet getRepoForVersion(String releaseType) {
        if ("BUILD-SNAPSHOT".equals(releaseType)) {
            return SNAPSHOT.toSet();
        }
        if (releaseType.startsWith("M") || releaseType.startsWith("RC")) {
            return MILESTONE.toSet();
        }
        return JkRepoSet.of();
    }

}
