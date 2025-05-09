package dev.jeka.core.api.utils;

import org.junit.jupiter.api.Test;

class JkUtilsThrowableTest {

    @Test
    public void printStackTrace() {
        Exception e = nestedException();
        e.printStackTrace();
        System.out.println("---------------");
        JkUtilsThrowable.printStackTrace(System.err, e, 2);
    }

    private Exception stackedException(int stackCount, Exception cause) {
        if (stackCount == 0) {
            return new IllegalArgumentException("My message", cause);
        }
        return stackedException(stackCount -1, cause);
    }

    private Exception nestedException() {
        return stackedException(10, stackedException(8, null));
    }
}