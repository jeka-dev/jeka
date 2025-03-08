package dev.jeka.core.api.function;

import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JkRunnablesTest {

    @Test
    void appendAppleAfterOrangeThenOrange_orangeWasNotPresent_appleIsAfterOrange() {
        Runnable dummyRunnable = () -> {};
        JkRunnables runnables = JkRunnables.of()
                .append("lime", dummyRunnable)
                .insertAfter("apple", "orange", dummyRunnable)
                .append("orange", dummyRunnable);
        assertEquals("orange", runnables.getRunnableNames().get(1));
        assertEquals("apple", runnables.getRunnableNames().get(2));
    }

    @Test
    void appendAppleBeforeOrange_orangeWasPresent_appleIsBeforeOrange() {
        Runnable dummyRunnable = () -> {};
        JkRunnables runnables = JkRunnables.of()
                .append("lime", dummyRunnable)
                .append("orange", dummyRunnable)
                .insertBefore("apple", "orange" ,dummyRunnable);
        assertEquals("apple", runnables.getRunnableNames().get(1));
        assertEquals("orange", runnables.getRunnableNames().get(2));
    }

    @Test
    void appendAnonymousRunnable_nameNotNull() {
        Runnable dummyRunnable = () -> {};
        JkRunnables runnables = JkRunnables.of()
                .append( dummyRunnable);
        assertNotNull(runnables.getRunnableNames().get(0));
    }

    @Test
    void insertBefore() {
        Runnable a = () -> {
            System.out.println("A");
        };
        Runnable b = () -> {
            System.out.println("B");
        };
        Runnable c = () -> {
            System.out.println("C");
        };
        JkRunnables runnables = JkRunnables.of()
                .append("b",b)
                .append("c", c)
                .insertBefore("a", "b", a);
        runnables.run();
        List<String> names = runnables.getRunnableNames();
        assertEquals(JkUtilsIterable.listOf("a", "b", "c"), names);
    }
}