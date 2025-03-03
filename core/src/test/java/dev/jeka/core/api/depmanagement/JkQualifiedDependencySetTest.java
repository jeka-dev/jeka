package dev.jeka.core.api.depmanagement;


import org.junit.Assert;
import org.junit.Test;

import static dev.jeka.core.api.depmanagement.JkPopularLibs.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularLibs.JAVAX_SERVLET_API;

public class JkQualifiedDependencySetTest {

    @Test
    public void computeIdeDependencies_1compileAnd1provided_ok() {
        JkDependencySet compile = JkDependencySet.of()
                .and(GUAVA.toCoordinate("19.0").toString())
                .and (JAVAX_SERVLET_API.toCoordinate("4.0.1").toString());
        JkDependencySet runtime = compile.minus(JAVAX_SERVLET_API);
        JkQualifiedDependencySet qdeps = JkQualifiedDependencySet.computeIdeDependencies(compile, runtime, JkDependencySet.of());
        JkQualifiedDependency guava = qdeps.findByCoordinateGroupId(GUAVA.toString()).get(0);
        JkQualifiedDependency servlet = qdeps.findByCoordinateGroupId(JAVAX_SERVLET_API.toString()).get(0);
        Assert.assertEquals("compile", guava.getQualifier() );
        Assert.assertEquals("provided", servlet.getQualifier() );
    }


}
