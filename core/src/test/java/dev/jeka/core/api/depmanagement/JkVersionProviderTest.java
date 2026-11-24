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

    @Test
    void andBom_allNotationsAreEquivalent() {
        JkCoordinate expected = JkVersionProvider.of().andBom("g:n:1").getBoms().iterator().next();
        assertEquals(expected, JkVersionProvider.of().andBom("g:n:1@pom").getBoms().iterator().next());
        assertEquals(expected, JkVersionProvider.of().andBom("g:n::pom:1").getBoms().iterator().next());
        assertEquals("1", expected.getVersion().getValue());
    }

}
