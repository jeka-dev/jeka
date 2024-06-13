package dev.jeka.core.api.function;

import org.junit.Assert;
import org.junit.Test;

public class JkRunnablesTest {

    @Test
    public void appendAppleAfterOrangeThenOrange_orangeWasNotPresent_appleIsAfterOrange() {
        Runnable dummyRunnable = () -> {};
        JkRunnables runnables = JkRunnables.of()
                .append("lime", dummyRunnable)
                .insertAfter("apple", "orange", dummyRunnable)
                .append("orange", dummyRunnable);
        Assert.assertEquals("orange", runnables.getRunnableNames().get(1));
        Assert.assertEquals("apple", runnables.getRunnableNames().get(2));
    }

    @Test
    public void appendAppleBeforeOrange_orangeWasPresent_appleIsBeforeOrange() {
        Runnable dummyRunnable = () -> {};
        JkRunnables runnables = JkRunnables.of()
                .append("lime", dummyRunnable)
                .append("orange", dummyRunnable)
                .insertBefore("apple", "orange" ,dummyRunnable);
        Assert.assertEquals("apple", runnables.getRunnableNames().get(1));
        Assert.assertEquals("orange", runnables.getRunnableNames().get(2));
    }

    @Test
    public void appendAnnonymousRunnable_nameNotNull() {
        Runnable dummyRunnable = () -> {};
        JkRunnables runnables = JkRunnables.of()
                .append( dummyRunnable);
        Assert.assertNotNull(runnables.getRunnableNames().get(0));
    }

}