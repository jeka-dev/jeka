package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;


public class JkCoordinateTest {

    @Test
    public void testTestEquals() {
        String description = "mygroup:myname:myVersion";
        Assert.assertEquals(JkCoordinate.of(description), JkCoordinate.of(description));
    }
}