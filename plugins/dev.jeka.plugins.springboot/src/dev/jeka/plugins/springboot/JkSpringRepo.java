package dev.jeka.plugins.springboot;


import dev.jeka.core.api.depmanagement.JkRepo;

/**
 * Download repositories of <i>Spring IO</i> company.
 */
public enum JkSpringRepo {

    SNAPSHOT("https://repo.spring.io/snapshot/"),
    MILESTONE("https://repo.spring.io/milestone/"),
    RELEASE("https://repo.spring.io/release/");

    private final String url;

    JkSpringRepo(String url) {
        this.url = url;
    }

    public JkRepo get() {
        return JkRepo.of(url);
    }

}
