package dev.jeka.core.api.depmanagement;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JkDependencySetTest {

    @Test
    void normalised_takeHighest_ok() {
        JkDependencySet dependencySet = JkDependencySet.of()
                .and("my.group:module:2.0")
                    .withLocalExclusions("a_transitive:dependency")
                .and("my.group:module:2.8")
                .andFiles("./foo.jar")
                .and("my.group:module")
                .and("other:other")
                .normalised(JkCoordinate.ConflictStrategy.TAKE_HIGHEST);
        Assertions.assertEquals("2.8", dependencySet.get("my.group:module").getCoordinate().getVersion().toString());
    }

    @Test
    void and_modules_ok() {
        final JkDependencySet deps = JkDependencySet.of()
                .and("hibernate:hjmlm:1212.0")
                .and("spring:spring:6.3")
                .and(secondaryDeps())
                .and("klklkl:lklk:mlml")
                .and("hhhhh:ll:ppp");
        Assertions.assertEquals(secondaryDeps().getEntries().size() + 4, deps.getEntries().size());
    }

    private static JkDependencySet secondaryDeps() {
        return JkDependencySet.of().and("454545:5445:54545").and("lkll:llljk:poo");
    }

}
