package dev.jeka.core.api.depmanagement;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JkVersionProviderTest {

    @Test
    void withExactAndWildCard_returnExact() {
        JkVersionProvider provider = JkVersionProvider.of()
                .and("foo:bar", "1.0.0")
                .and("foo:*", "2.0.0");
        assertEquals("1.0.0", provider.getVersionOf("foo:bar"));
        assertEquals("2.0.0", provider.getVersionOf("foo:other"));
        assertNull(provider.getVersionOf("my-group:my-name"));

    }

}
