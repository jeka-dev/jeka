package dev.jeka.examples.capitalizer;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CapitalizerTest {

    @Test
    public void test() {
        String sample = "hello world";
        Assertions.assertEquals("Hello World", Capitalizer.capitalize(sample));
    }

}
