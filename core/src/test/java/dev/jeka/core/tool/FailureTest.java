package dev.jeka.core.tool;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class FailureTest {

    @Test
    void doAssertFailure() {
        Assertions.fail("it must fail");
    }

    @Test
    void doExceptionraise() {
        throw new RuntimeException(new RuntimeException("exception2"));
    }

    @Test
    @Disabled
    void doIgnore() {
        throw new RuntimeException();
    }

}
