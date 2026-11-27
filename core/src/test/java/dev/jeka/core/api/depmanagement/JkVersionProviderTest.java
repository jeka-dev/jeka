package dev.jeka.core.api.depmanagement;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void equalsAndHashCode_differentBoms_notEqual() {
        JkVersionProvider bom1 = JkVersionProvider.of().andBom("com.fasterxml.jackson:jackson-bom:2.17.0");
        JkVersionProvider bom2 = JkVersionProvider.of().andBom("com.fasterxml.jackson:jackson-bom:2.18.0");
        assertNotEquals(bom1, bom2);
        assertNotEquals(bom1.hashCode(), bom2.hashCode());
        assertEquals(bom1, JkVersionProvider.of().andBom("com.fasterxml.jackson:jackson-bom:2.17.0"));
        assertEquals(bom1.hashCode(), JkVersionProvider.of().andBom("com.fasterxml.jackson:jackson-bom:2.17.0").hashCode());
    }

    @Test
    void equals_bomsInAnotherOrder_notEqual() {  // the first BOM wins, so the order matters
        JkVersionProvider provider = JkVersionProvider.of().andBom("g:bom1:1").andBom("g:bom2:1");
        assertNotEquals(provider, JkVersionProvider.of().andBom("g:bom2:1").andBom("g:bom1:1"));
    }

}
