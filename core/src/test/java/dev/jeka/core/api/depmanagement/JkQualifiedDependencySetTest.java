package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;

import static dev.jeka.core.api.depmanagement.JkPopularLibs.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularLibs.JAVAX_SERVLET_API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JkQualifiedDependencySetTest {

    @Test
    void computeIdeDependencies_1compileAnd1provided_ok() {
        JkDependencySet compile = JkDependencySet.of()
                .and(GUAVA.toCoordinate("19.0").toString())
                .and (JAVAX_SERVLET_API.toCoordinate("4.0.1").toString());
        JkDependencySet runtime = compile.minus(JAVAX_SERVLET_API);
        JkQualifiedDependencySet qdeps = JkQualifiedDependencySet.computeIdeDependencies(compile, runtime, JkDependencySet.of());
        JkQualifiedDependency guava = qdeps.findByCoordinateGroupId(GUAVA.toString()).get(0);
        JkQualifiedDependency servlet = qdeps.findByCoordinateGroupId(JAVAX_SERVLET_API.toString()).get(0);
        assertEquals("compile", guava.getQualifier() );
        assertEquals("provided", servlet.getQualifier() );
    }

    // The md5 is the key of the dependency resolution cache: it must change when only a BOM changes
    @Test
    void md5_onlyBomChanges_differs() {
        JkDependencySet deps = JkDependencySet.of().and("com.fasterxml.jackson.core:jackson-databind");
        JkQualifiedDependencySet withBom1 = JkQualifiedDependencySet.of(deps
                .withVersionProvider(JkVersionProvider.of().andBom("com.fasterxml.jackson:jackson-bom:2.17.0")));
        JkQualifiedDependencySet withBom2 = JkQualifiedDependencySet.of(deps
                .withVersionProvider(JkVersionProvider.of().andBom("com.fasterxml.jackson:jackson-bom:2.18.0")));
        assertNotEquals(withBom1.md5(), withBom2.md5());
    }

}
