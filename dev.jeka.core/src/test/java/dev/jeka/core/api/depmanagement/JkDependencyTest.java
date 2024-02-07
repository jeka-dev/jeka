package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class JkDependencyTest {


    @Test
    public void ofCoordinateDescription_providesCoordinateDeps() {
        Assert.assertEquals(JkCoordinateDependency.class,
                JkDependency.of(Paths.get(""), "commons-lang:commons-lang:2.63").getClass());
        Assert.assertEquals(JkCoordinateDependency.class,
                JkDependency.of(Paths.get(""), "dev.jeka:a-jeka-module").getClass());
    }

}
