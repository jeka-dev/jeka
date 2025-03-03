package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JkVersionProviderTest {

    @Test
    public void withExactAndWildCard_returnExact() {
        JkVersionProvider provider = JkVersionProvider.of()
                .and("foo:bar", "1.0.0")
                .and("foo:*", "2.0.0");
        assertEquals("1.0.0", provider.getVersionOf("foo:bar"));
        assertEquals("2.0.0", provider.getVersionOf("foo:other"));
        assertNull(provider.getVersionOf("my-group:my-name"));

    }

}
