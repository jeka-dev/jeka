package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JkDependencyTest {


    @Test
    public void ofCoordinateDescription_providesCoordinateDeps() {
       assertEquals(JkCoordinateDependency.class,
                JkDependency.of(Paths.get(""), "commons-lang:commons-lang:2.63").getClass());
       assertEquals(JkCoordinateDependency.class,
                JkDependency.of(Paths.get(""), "dev.jeka:a-jeka-module").getClass());
    }

}
